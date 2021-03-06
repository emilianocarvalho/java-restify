package com.github.ljtfreitas.restify.http.contract;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.ljtfreitas.restify.http.contract.DefaultRestifyContract;
import com.github.ljtfreitas.restify.http.contract.metadata.EndpointMethod;
import com.github.ljtfreitas.restify.http.contract.metadata.EndpointTarget;
import com.github.ljtfreitas.restify.http.contract.metadata.EndpointType;
import com.github.ljtfreitas.restify.http.contract.metadata.RestifyContractReader;

@RunWith(MockitoJUnitRunner.class)
public class DefaultRestifyContractTest {

	@Mock
	private RestifyContractReader restifyContractReaderMock;

	@InjectMocks
	private DefaultRestifyContract contract;

	private EndpointTarget endpointTarget;

	@Before
	public void setup() {
		endpointTarget = new EndpointTarget(MyApiType.class);

		when(restifyContractReaderMock.read(same(endpointTarget), any(Method.class)))
			.then(invocation -> new EndpointMethod(invocation.getArgumentAt(1, Method.class), "/path", "GET"));
	}

	@Test
	public void shouldReadMetadataOfMethodWithSingleParameter() throws Exception {
		EndpointType endpointType = contract.read(endpointTarget);

		assertEquals(MyApiType.class, endpointType.javaType());

		Method javaMethod = MyApiType.class.getMethod("method");

		Optional<EndpointMethod> endpointMethod = endpointType.find(javaMethod);
		assertTrue(endpointMethod.isPresent());

		assertEquals(javaMethod, endpointMethod.get().javaMethod());

		verify(restifyContractReaderMock).read(endpointTarget, javaMethod);
	}
	
	private interface MyApiType {

		String method();
	}
}
