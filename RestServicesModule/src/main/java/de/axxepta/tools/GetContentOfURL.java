package de.axxepta.tools;

import java.io.IOException;

import de.axxepta.tools.interfaces.IGetContentFromUrl;
import ro.sync.basic.util.HTTPUtil;

public class GetContentOfURL {

	public static IGetContentFromUrl getContent = (url) -> {
		try {
			return new String(HTTPUtil.getContentOfPage(url));
		} catch (IOException e) {
			return null;
		}
	};
}
