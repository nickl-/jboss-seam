package org.jboss.seam.ui.handler;

import java.io.IOException;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;

/**
 * Decorating handler 
 * 
 * @author mnovotny
 *
 */
public class DecorateHandler extends ComponentHandler {
	
   private com.sun.faces.facelets.tag.ui.DecorateHandler delegate;

	public DecorateHandler(ComponentConfig config) {
		super(config);
		if (this.tag.getAttributes().get("template") != null) {
			this.delegate = new com.sun.faces.facelets.tag.ui.DecorateHandler(config);
		}
	}
   
   @Override
	public void applyNextHandler(FaceletContext context, UIComponent component) throws IOException, FacesException,
			ELException {
		TagAttribute template = this.tag.getAttributes().get("template");
		if (template != null) {
			try {
				this.delegate.apply(context, component);
			} catch (Exception e) {				
				throw new IOException("Could not load template:" + getValue(template, context), e);
			}
		} else {
			this.nextHandler.apply(context, component);
		}
	}

	private String getValue(TagAttribute template, FaceletContext context) {
		assert template != null;
		try {
			return template.getValue(context);
		}
		catch (Exception e) {
			return template.getValue();
		}
	}

}
