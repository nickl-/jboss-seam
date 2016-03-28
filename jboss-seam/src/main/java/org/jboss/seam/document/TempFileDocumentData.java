package org.jboss.seam.document;

import java.io.File;

/**
 * Stores DocumentData in a file, delete at end when DocumentStore is destroyed.
 *
 */
public class TempFileDocumentData extends FileDocumentData {

	public TempFileDocumentData (String baseName, DocumentType documentType, File data) {
		super(baseName, documentType, data);
	}
	
	public void cleanup() {
		File file = getData();
		if (file != null && file.exists()) {
			file.delete();

		}
	}

}
