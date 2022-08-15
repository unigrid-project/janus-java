/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.unigrid.janus.model.entity;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class MyMessageProviderReader implements MessageBodyReader<GithubJson>{

	@Override
	public boolean isReadable(Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
		return type == GithubJson.class;
	}

	@Override
	public GithubJson readFrom(Class<GithubJson> type, Type type1, Annotation[] antns, MediaType mt,
		MultivaluedMap<String, String> mm, InputStream in) 
		throws IOException, WebApplicationException {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(GithubJson.class);
			GithubJson githubJson = (GithubJson) jaxbContext.createUnmarshaller().unmarshal(in);
			return githubJson;
		} catch (JAXBException e) {
			throw new IOException("Error deserilizing a GitHubJason xml object", e);
		}
	}
	
}
