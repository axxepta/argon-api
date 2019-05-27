package de.axxepta.tools.interfaces;

import org.w3c.dom.Document;

@FunctionalInterface
public interface IDomFromString {

	public Document getDOM (String content);
}

