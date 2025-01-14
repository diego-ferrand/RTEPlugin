package com.blazemeter.jmeter.rte.protocols;

import com.blazemeter.jmeter.rte.core.Segment;
import com.blazemeter.jmeter.rte.core.ServerDisconnectHandler;
import com.blazemeter.jmeter.rte.core.RteProtocolClient;
import com.blazemeter.jmeter.rte.core.Screen;
import com.blazemeter.jmeter.rte.core.TerminalType;
import com.blazemeter.jmeter.rte.core.exceptions.ConnectionClosedException;
import com.blazemeter.jmeter.rte.core.listener.ExceptionHandler;
import com.blazemeter.jmeter.rte.core.ssl.SSLType;
import com.blazemeter.jmeter.rte.core.wait.SyncWaitCondition;
import com.blazemeter.jmeter.rte.protocols.tn5250.Tn5250ClientIT;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.abstracta.wiresham.Flow;
import us.abstracta.wiresham.VirtualTcpService;

public abstract class RteProtocolClientIT<T extends RteProtocolClient> {

  protected static final Dimension SCREEN_SIZE = new Dimension(80, 24);
  protected static final String VIRTUAL_SERVER_HOST = "localhost";
  protected static final int TIMEOUT_MILLIS = 5000;
  protected static final int STABLE_TIMEOUT_MILLIS = 2000;
  protected static final long SERVER_STOP_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
  private static final Logger LOG = LoggerFactory.getLogger(Tn5250ClientIT.class);
  protected VirtualTcpService server = new VirtualTcpService();
  protected T client;

  protected BooleanProvider isDisconnectionExpected = new BooleanProvider();

  public static class BooleanProvider implements Supplier<Boolean> {

    private boolean value;

    public void update(boolean newValue) {
      this.value = newValue;
    }

    @Override
    public Boolean get() {
      return value;
    }
  }

  @Before
  public void setup() throws Exception {
    server.setSslEnabled(false);
    server.start();
    client = buildClient();
    client.setDisconnectionHandler(buildDisconnectionHandler());
  }

  private ServerDisconnectHandler buildDisconnectionHandler() {
    return new ServerDisconnectHandler(this.isDisconnectionExpected.get()) {
      @Override
      public void onDisconnection(ExceptionHandler handler) {
        if (!isDisconnectionExpected.get()) {
          handler.setPendingError(new ConnectionClosedException());
        }
      }
    };
  }

  protected abstract T buildClient();

  @After
  public void teardown() throws Exception {
    try {
      if (client != null) {
        client.disconnect();
      }
    } catch (Exception e) {
      LOG.warn("Problem disconnecting client", e);
    } finally {
      if (server != null) {
        server.stop(SERVER_STOP_TIMEOUT);
      }
    }
  }

  protected void loadFlow(String flowFile) throws FileNotFoundException {
    File file = new File(findResource(flowFile).getFile());
    server.setFlow(Flow.fromYml(file));
  }

  protected URL findResource(String file) {
    return getClass().getResource(file);
  }

  protected void connectToVirtualService() throws Exception {
    client.connect(VIRTUAL_SERVER_HOST, server.getPort(), SSLType.NONE, getDefaultTerminalType(),
        TIMEOUT_MILLIS);
    client.await(
        Collections.singletonList(new SyncWaitCondition(TIMEOUT_MILLIS, STABLE_TIMEOUT_MILLIS)));
  }

  protected abstract TerminalType getDefaultTerminalType();

  protected Screen buildScreenFromHtmlFile(String fileName) throws IOException {
    return Screen.fromHtml(Resources.toString(findResource(fileName), Charsets.UTF_8));
  }

  protected abstract List<Segment> buildExpectedFields();

  protected Segment.SegmentBuilder editableSegment() {
    return new Segment.SegmentBuilder().withEditable().withColor(Color.BLACK);
  }

}
