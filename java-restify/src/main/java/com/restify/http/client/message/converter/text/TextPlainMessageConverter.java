package com.restify.http.client.message.converter.text;

public class TextPlainMessageConverter extends TextMessageConverter {

	private static final String TEXT_PLAIN = "text/plain";

	@Override
	public String contentType() {
		return TEXT_PLAIN;
	}

}