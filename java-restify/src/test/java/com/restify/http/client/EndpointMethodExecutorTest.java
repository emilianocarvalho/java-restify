package com.restify.http.client;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.restify.http.client.call.EndpointCall;
import com.restify.http.client.call.EndpointCallFactory;
import com.restify.http.client.call.exec.EndpointCallExecutable;
import com.restify.http.client.call.exec.EndpointCallExecutables;
import com.restify.http.contract.metadata.EndpointMethod;
import com.restify.http.contract.metadata.reflection.JavaType;

@RunWith(MockitoJUnitRunner.class)
public class EndpointMethodExecutorTest {

	@Mock
	private EndpointCallExecutables endpointMethodExecutablesMock;

	@Mock
	private EndpointCallExecutable<Object, Object> endpointMethodExecutableMock;

	@Mock
	private EndpointCallFactory endpointMethodCallFactoryMock;

	@InjectMocks
	private EndpointMethodExecutor endpointMethodExecutor;

	private EndpointMethod endpointMethod;

	@Before
	public void setup() throws NoSuchMethodException, SecurityException {
		endpointMethod = new EndpointMethod(SomeType.class.getMethod("method"), "http://my.api.com/", "GET");

		when(endpointMethodExecutablesMock.of(endpointMethod))
			.thenReturn(endpointMethodExecutableMock);

		SimpleEndpointMethodCall call = new SimpleEndpointMethodCall("endpoint result");

		when(endpointMethodCallFactoryMock.createWith(notNull(EndpointMethod.class), any(), notNull(JavaType.class)))
			.thenReturn(call);

		when(endpointMethodExecutableMock.execute(call))
			.then(i -> i.getArgumentAt(0, EndpointCall.class).execute());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldExecuteEndpointMethod() throws Exception {
		Object[] args = new Object[]{"arg"};

		JavaType returnType = JavaType.of(String.class);

		when(endpointMethodExecutableMock.returnType())
			.thenReturn(returnType);

		Object result = endpointMethodExecutor.execute(endpointMethod, args);

		assertEquals("endpoint result", result);

		verify(endpointMethodExecutablesMock).of(endpointMethod);
		verify(endpointMethodCallFactoryMock).createWith(notNull(EndpointMethod.class), any(), notNull(JavaType.class));
		verify(endpointMethodExecutableMock).execute(notNull(EndpointCall.class));
	}

	interface SomeType {
		String method();
	}

	private class SimpleEndpointMethodCall implements EndpointCall<Object> {

		private final Object result;

		private SimpleEndpointMethodCall(Object result) {
			this.result = result;
		}

		@Override
		public Object execute() {
			return result;
		}
	}
}
