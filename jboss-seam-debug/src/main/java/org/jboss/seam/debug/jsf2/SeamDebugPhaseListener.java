package org.jboss.seam.debug.jsf2;

import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.jboss.seam.contexts.FacesLifecycle;

/**
 * Intercepts any request for a view-id like /debug.xxx and renders
 * the Seam debug page using facelets.
 * 
 * @author Gavin King
 */
public class SeamDebugPhaseListener implements PhaseListener {

	private static final long serialVersionUID = -1008115434573988880L;

	public SeamDebugPhaseListener() {
		super();
	}

	public void beforePhase(PhaseEvent event) {
		FacesLifecycle.setPhaseId(event.getPhaseId()); // since this gets called
														// before
														// SeamPhaseListener!
	}

	public void afterPhase(PhaseEvent event) {
	}

	public PhaseId getPhaseId() {
		return PhaseId.RENDER_RESPONSE;
	}

}
