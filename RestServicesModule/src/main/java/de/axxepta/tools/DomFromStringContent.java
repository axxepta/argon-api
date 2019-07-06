package de.axxepta.tools;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.wst.xml.xpath2.processor.DOMBuilder;
import org.eclipse.wst.xml.xpath2.processor.DOMLoaderException;

import de.axxepta.tools.interfaces.IDomFromString;

public class DomFromStringContent {
	
	public static IDomFromString domFromString = (content) -> {
		DOMBuilder domBuilder = new DOMBuilder();
		InputStream stream = new ByteArrayInputStream(content.getBytes());
		try {
			return domBuilder.load(stream);
		} catch (DOMLoaderException e) {
			return null;
		}
	};
}
