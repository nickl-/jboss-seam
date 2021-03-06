/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.seam.contexts;

import java.util.Map;
import java.util.Set;

import org.jboss.seam.ScopeType;
import org.jboss.seam.core.ConversationEntries;
import org.jboss.seam.core.Manager;
import org.jboss.seam.log.LogProvider;
import org.jboss.seam.log.Logging;

/**
 * Methods for setup and teardown of Seam contexts.
 * 
 * @author Gavin King
 * @author <a href="mailto:theute@jboss.org">Thomas Heute</a>
 */
public class Lifecycle
{

   private static final LogProvider log = Logging.getLogProvider(Lifecycle.class);

   private static ThreadLocal<Boolean> destroying = new ThreadLocal<Boolean>();
   private static ThreadLocal<Map<String, Object>> application = new ThreadLocal<Map<String, Object>>();

   public static Map<String, Object> getApplication() 
   {
      if (!isApplicationInitialized())
      {
         throw new IllegalStateException("Attempted to invoke a Seam component outside an initialized application");
      }
      return application.get();
   }   
   
   public static boolean isApplicationInitialized() {
       // FIXME Check..
       //return (application.get() != null);
       return true;
   }

   public static void beginApplication(Map<String, Object> app) 
   {
      application.set(app);
   }
   
   public static void endApplication()
   {
      endApplication(application.get());
   }
   
   public static void endApplication(Map<String,Object> app)
   {
      log.debug("Shutting down application and destroying contexts");
      
      Context tempApplicationContext = new ApplicationContext( app );
      Contexts.applicationContext.set(tempApplicationContext);
      Contexts.destroy(tempApplicationContext);
      Contexts.applicationContext.set(null);
      Contexts.eventContext.set(null);
      Contexts.sessionContext.set(null);
      Contexts.conversationContext.set(null);
      
      application.set(null);
   }

   public static void startDestroying()
   {
      destroying.set(true);
   }

   public static void stopDestroying()
   {
      destroying.set(false);
   }

   public static boolean isDestroying()
   {
      Boolean value = destroying.get();
      return value!=null && value.booleanValue();
   }

   public static void beginCall()
   {
      log.debug( ">>> Begin call" );
      Contexts.applicationContext.set( new ApplicationContext(getApplication()) );
      Contexts.eventContext.set( new BasicContext(ScopeType.EVENT) );
      Contexts.sessionContext.set( new BasicContext(ScopeType.SESSION) );
      Contexts.conversationContext.set( new BasicContext(ScopeType.CONVERSATION) );
      Contexts.businessProcessContext.set( new BusinessProcessContext() );
   }

   public static void endCall()
   {
      try
      {
         Contexts.destroy( Contexts.getSessionContext() );
         Contexts.flushAndDestroyContexts();
         if ( Manager.instance().isLongRunningConversation() )
         {
            throw new IllegalStateException("Do not start long-running conversations in direct calls to EJBs");
         }
      }
      finally
      {
         Contexts.clearThreadlocals();
         log.debug( "<<< End call" );
      }
   }

   /**
    * @deprecated Use {@link Lifecycle#setupApplication()}
    */
   @Deprecated
   public static void mockApplication()
   {
      setupApplication(null);
   }
   
   /**
    * @deprecated Use {@link Lifecycle#cleanupApplication()}
    */
   @Deprecated
   public static void unmockApplication()
   {
      cleanupApplication();
   }
   
   public static void setupApplication()
   {
      Contexts.applicationContext.set( new ApplicationContext(getApplication()) );
   }
   
   public static void setupApplication(Map<String, Object> appCtx)
   {
         Contexts.applicationContext.set(new ApplicationContext(appCtx));
   }

   public static void cleanupApplication()
   {
      Contexts.applicationContext.set(null);
   }

   public static Context beginMethod()
   {
      Context result = Contexts.methodContext.get();
      Contexts.methodContext.set( new BasicContext(ScopeType.METHOD) );
      return result;
   }

   public static void endMethod(Context context)
   {
      Contexts.methodContext.set(context);
   }
   
   public static void endRequest() 
   {
      log.debug("After request, destroying contexts");  
      try
      {
         Contexts.flushAndDestroyContexts();
      }
      finally
      {
         Contexts.clearThreadlocals();
         log.debug( "<<< End web request" );
      }
   }


   
   public static void destroyConversationContext(Map<String, Object> session, String conversationId)
   {
      Contexts.destroyConversationContext(session, conversationId);
   }

   @Deprecated
   public static void beginSession(Map<String, Object> session)
   {
      beginSession(session,null);
   }
   
   public static void beginSession(Map<String, Object> session, Map<String,Object> appCtx)
   {
      log.debug("Session started");
      
      //Normally called synchronously with a JSF request, but there are some
      //special cases!

      boolean applicationContextActive = Contexts.isApplicationContextActive();
      boolean eventContextActive = Contexts.isEventContextActive();
      boolean conversationContextActive = Contexts.isConversationContextActive();

      if ( !applicationContextActive )
      {
         Context tempApplicationContext = null;
         if(appCtx == null)
         {
            tempApplicationContext= new ApplicationContext( getApplication() );
         }
         else
         {
            tempApplicationContext = new ApplicationContext(appCtx);
         }
         Contexts.applicationContext.set(tempApplicationContext);
      }
      Context oldSessionContext = Contexts.sessionContext.get();
      Contexts.sessionContext.set( new SessionContext(session) ); //we have to use the session object that came in the sessionCreated() event
      Context tempEventContext = null;
      if ( !eventContextActive )
      {
         tempEventContext = new BasicContext(ScopeType.EVENT);
         Contexts.eventContext.set(tempEventContext);
      }
      Context tempConversationContext = null;
      if ( !conversationContextActive )
      {
         tempConversationContext = new BasicContext(ScopeType.CONVERSATION);
         Contexts.conversationContext.set(tempConversationContext);
      }

      Contexts.startup(ScopeType.SESSION);
      
      if ( !conversationContextActive )
      {
         Contexts.destroy(tempConversationContext);
         Contexts.conversationContext.set(null);
      }
      if ( !eventContextActive ) 
      {
         Contexts.destroy(tempEventContext);
         Contexts.eventContext.set(null);
      }
      Contexts.sessionContext.set(oldSessionContext); //replace the one from sessionCreated() with the one from JSF, or null
      if ( !applicationContextActive ) 
      {
         Contexts.applicationContext.set(null);
      }
      
   }
   public static void endSession(Map<String, Object> session)
   {
      endSession(session, application.get());
   }
         
   public static void endSession(Map<String, Object> session, Map<String,Object> app)
   {
      log.debug("End of session, destroying contexts");
      
      //This code assumes that sessions are only destroyed at the very end of a  
      //web request, after the request-bound context objects have been destroyed,
      //or during session timeout, when there are no request-bound contexts.
      
      if ( Contexts.isEventContextActive() || Contexts.isApplicationContextActive() ) {
         log.debug("Please end the HttpSession via org.jboss.seam.web.Session.instance().invalidate()");
      }
      try {
	      Context tempApplicationContext = new ApplicationContext( app );
	      Contexts.applicationContext.set(tempApplicationContext);
	   
	      //this is used just as a place to stick the ConversationManager
	      Context tempEventContext = new BasicContext(ScopeType.EVENT);
	      Contexts.eventContext.set(tempEventContext);
	   
	      //this is used (a) for destroying session-scoped components
	      //and is also used (b) by the ConversationManager
	      Context tempSessionContext = new SessionContext(session);
	      Contexts.sessionContext.set(tempSessionContext);
	   
	      Set<String> conversationIds = ConversationEntries.instance().getConversationIds();
	      if (log.isDebugEnabled()){
	          log.debug("destroying conversation contexts: " + conversationIds);
	      }
	      for (String conversationId: conversationIds) {
	         Contexts.destroyConversationContext(session, conversationId);
	      }
	      
	      //we need some conversation-scope components for destroying
	      //the session context...
	      Context tempConversationContext = new BasicContext(ScopeType.CONVERSATION);
	      Contexts.conversationContext.set(tempConversationContext);
	   
	      log.debug("destroying session context");
	      Contexts.destroy(tempSessionContext);
	      Contexts.sessionContext.set(null);
	      
	      Contexts.destroy(tempConversationContext);
	      Contexts.conversationContext.set(null);
	   
	      Contexts.destroy(tempEventContext);
	      Contexts.eventContext.set(null);
	   
	      Contexts.applicationContext.set(null);
      }
      catch (Exception ex) {
    	  log.trace("Error destroying", ex);
      }
   }

}