package com.namnv;

import com.namnv.client.ClientEgressListener;
import com.namnv.client.ClientIngressSender;
import com.weareadaptive.sbe.MessageHeaderEncoder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerRequest;

public class HttpServer extends AbstractVerticle {
  ClientIngressSender clientIngressSender;
  ClientEgressListener clientEgressListener;
  private long id = 0L;
  final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();

  public HttpServer(final ClientIngressSender clientIngressSender,
                    final ClientEgressListener clientEgressListener) {
    this.clientIngressSender = clientIngressSender;
    this.clientEgressListener = clientEgressListener;
  }

  @Override
  public void start() {
    vertx.createHttpServer()
      .requestHandler(this::WSHandler)
      .listen(8080);
  }

  private void WSHandler(HttpServerRequest httpServerRequest) {
    String path = httpServerRequest.path();

    switch (path) {
      case "/transfer" -> transfer(httpServerRequest, ++id);
    }

  }


  private void transfer(HttpServerRequest httpServerRequest, long correlationId) {
    clientEgressListener.addHttpRequest(correlationId, httpServerRequest);
    clientIngressSender.sendOrderRequestToCluster(correlationId, 1, 1, 1);
  }

}
