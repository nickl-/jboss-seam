/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.seam.web;

import static org.jboss.seam.ScopeType.APPLICATION;
import static org.jboss.seam.annotations.Install.BUILT_IN;

import java.io.IOException;

import javax.faces.component.UIViewRoot;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.web.Filter;
import org.jboss.seam.contexts.FacesLifecycle;
import org.jboss.seam.core.Manager;
import org.jboss.seam.exception.Exceptions;
import org.jboss.seam.log.LogProvider;
import org.jboss.seam.log.Logging;
import org.jboss.seam.mock.MockApplication;
import org.jboss.seam.mock.MockExternalContext;
import org.jboss.seam.mock.MockFacesContext;
import org.jboss.seam.transaction.Transaction;
import org.jboss.seam.transaction.UserTransaction;
import org.jboss.seam.util.EJB;

/**
 * Delegate uncaught exceptions to Seam exception handling.
 * As a last line of defence, rollback uncommitted transactions,
 * and clean up Seam contexts.
 * 
 * @author Gavin King
 */
@Startup
@Scope(APPLICATION)
@Name("org.jboss.seam.web.exceptionFilter")
@Install(precedence = BUILT_IN, classDependencies="javax.faces.context.FacesContext")
@BypassInterceptors
@Filter(within="org.jboss.seam.web.ajax4jsfFilter")
public class ExceptionFilter extends AbstractFilter
{
   
   private static final LogProvider log = Logging.getLogProvider(ExceptionFilter.class);
   
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
         throws IOException, ServletException
   {
      try
      {
         chain.doFilter(request, response);
      }
      catch (Exception e)
      {
         log.error( "handling uncaught exception", e );
         log.error( "exception root cause", EJB.getCause(e) );
         endWebRequestAfterException( (HttpServletRequest) request, (HttpServletResponse) response, e);
      }
   }
   
   protected void endWebRequestAfterException(HttpServletRequest request, HttpServletResponse response, Exception e) 
         throws ServletException, IOException
   {
      //TODO: Are we really sure that someone flushes the "old" 
      //      conversation context before we get to here? I guess
      //      the phaselistener probably does it, but we want to
      //      make sure of that...
      
      log.debug("running exception handlers");
      //the FacesContext is gone - create a fake one for Redirect and HttpError to call
      MockFacesContext facesContext = createFacesContext(request, response);
      facesContext.setCurrent();
      
      //Init the temp context objects
      //TODO: note that this code is pretty dodgy since in theory 
      //      Manager has already been destroyed, and now we are 
      //      re-using it (and all other request-scoped objects).
      //      Should create a new request context that does not 
      //      map back to the servlet request context, and "copy"
      //      the conversation id over.
      FacesLifecycle.beginExceptionRecovery( facesContext.getExternalContext() );
      
      //if there is an existing long-running conversation on
      //the thread, propagate it
      Manager manager = Manager.instance();
      if ( !manager.isLongRunningOrNestedConversation() )
      {
         manager.initializeTemporaryConversation();
      }
      
      //Now do the exception handling
      try
      {
         rollbackTransactionIfNecessary();
         Exceptions.instance().handle(e);
      }
      catch (ServletException se)
      {
         throw se;
      }
      catch (IOException ioe)
      {
         throw ioe;
      }
      catch (Exception ehe)
      {
         throw new ServletException(ehe);
      }
      finally
      {
         //finally, clean up the temp contexts
         try 
         {
            FacesLifecycle.endRequest( facesContext.getExternalContext() );
            facesContext.release();
            log.debug("done running exception handlers");
         }
         catch (Exception ere)
         {
            log.error("could not destroy contexts", e);
         }
      }
   }
   
   private MockFacesContext createFacesContext(HttpServletRequest request, HttpServletResponse response)
   {
      MockFacesContext mockFacesContext = new MockFacesContext( new MockExternalContext(getServletContext(), request, response), new MockApplication() );
      mockFacesContext.setViewRoot( new UIViewRoot() );
      return mockFacesContext;
   }
   
   protected void rollbackTransactionIfNecessary()
   {
      try 
      {
         UserTransaction transaction = Transaction.instance();
         if ( transaction.isActiveOrMarkedRollback() || transaction.isRolledBack() )
         {
            log.debug("killing transaction");
            transaction.rollback();
         }
      }
      catch (Exception te)
      {
         log.error("could not roll back transaction", te);
      }
   }
}
