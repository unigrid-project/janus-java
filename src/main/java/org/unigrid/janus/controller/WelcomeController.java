package org.unigrid.janus.controller;

import java.io.Serializable;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import lombok.Data;

@Data
@Named
@ViewScoped
public class WelcomeController implements Serializable {
	private String name;
	private String sir = "testytestaa";

	public String getName(String name) {
		return name + "momodd";
	}

	public void setName(String name) {
		//logger.info(name);
		this.name = name + "MMx";
	}

	public void onPress() {
		//logger.info("Pickle pressed the button");
	}
}
