package com.jslib.wood.apps;

import java.io.File;

import js.log.Log;
import js.log.LogFactory;
import js.tiny.container.core.AppContext;

final class ApacheHttpd {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(ApacheHttpd.class);

	private File DOC_ROOT;

	public ApacheHttpd(AppContext context) {
		log.trace("ApacheHttpdImpl(AppContext)");
		String docRootProperty = context.getProperty("server.doc.root");
		if (docRootProperty == null) {
			log.info("Missing <server.doc.root> environment property. Server configured without doc root. It can be added to conf/properties.xml file, e.g. <property name='server.doc.root' value='/var/www/vhosts/' />");
			return;
		}
		DOC_ROOT = new File(docRootProperty);
	}

	public File getFile(String path) {
		return new File(DOC_ROOT, path);
	}
}
