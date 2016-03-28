package org.jboss.seam.document;

import java.io.IOException;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.log.LogProvider;
import org.jboss.seam.log.Logging;
import org.jboss.seam.navigation.Pages;
import org.jboss.seam.web.Parameters;

public class DocumentStorePhaseListener implements PhaseListener
{
   private static final long serialVersionUID = 7308251684939658978L;

   private static final LogProvider log = Logging.getLogProvider(DocumentStorePhaseListener.class);

   public PhaseId getPhaseId()
   {
      return PhaseId.RENDER_RESPONSE;
   }

   public void afterPhase(PhaseEvent phaseEvent)
   {
      // ...
   }

   public void beforePhase(PhaseEvent phaseEvent)
   {
      String rootId = Pages.getViewId(phaseEvent.getFacesContext());

      Parameters params = Parameters.instance();
      String id = (String) params.convertMultiValueRequestParameter(params.getRequestParameters(), "docId", String.class);
      if (rootId.contains(DocumentStore.DOCSTORE_BASE_URL))
      {
         sendContent(phaseEvent.getFacesContext(), id);
      }
   }

   public void sendContent(FacesContext context, String contentId)
   {
      try
      {
         DocumentData documentData = DocumentStore.instance().getDocumentData(contentId);

         if (documentData != null)
         {

            HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
            HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
            response.setContentType(documentData.getDocumentType().getMimeType());

            response.setHeader("Content-Disposition", documentData.getDisposition() + "; filename=\"" + documentData.getFileName() + "\"");
            setHeadersForInternetExplorer(request, response);
            documentData.writeDataToStream(response.getOutputStream());
            context.responseComplete();
         }
      }
      catch (IOException e)
      {
         log.warn(e);
      }
   }
   protected static void setHeadersForInternetExplorer(HttpServletRequest request, HttpServletResponse response){
       if (request == null) {
           return;
       }
       if (request.isSecure() && isIE(request)){
           response.setHeader("Pragma", "a");
           response.setHeader("Cache-Control", "max-age=0");
           response.addHeader("Cache-Control", "must-revalidate");
       }
   }
   private static boolean isIE (HttpServletRequest request) {        
       assert request != null;
       String useragent = request.getHeader("User-Agent");
       boolean isIE = useragent != null && useragent.contains("MSIE");
       return isIE;
   }
}
