package io.crnk.brave;

import io.crnk.client.http.okhttp.OkHttpAdapter;

public class OkHttpBraveModuleTest extends AbstractBraveModuleTest {

	public OkHttpBraveModuleTest() {
		super(OkHttpAdapter.newInstance());
	}
}
