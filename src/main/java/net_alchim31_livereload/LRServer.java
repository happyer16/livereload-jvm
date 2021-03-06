package net_alchim31_livereload;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.resource.Resource;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class LRServer {
  private final int _port;
  private final Path _docroot;
  private Server _server;
  private Watcher _watcher;
  private static String[] _exclusions;

  public LRServer(int port, Path docroot) {
    this._port = port;
    this._docroot = docroot;
  }

  private void init() throws Exception {
    SelectChannelConnector connector = new SelectChannelConnector();
    connector.setPort(_port);

    ResourceHandler rHandler = new ResourceHandler() {
      @Override
      public Resource getResource(String path) throws MalformedURLException {
        if ("/livereload.js".equals(path)) {
          try {
            return Resource.newResource(LRServer.class.getResource(path));
          } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
          }
        }
        return super.getResource(path);
      }
    };
    rHandler.setDirectoriesListed(true);
    rHandler.setWelcomeFiles(new String[]{"index.html"});
    rHandler.setResourceBase(_docroot.toString());

    LRWebSocketHandler wsHandler = new LRWebSocketHandler();
    wsHandler.setHandler(rHandler);

    _server = new Server();
    _server.setHandler(wsHandler);
    _server.addConnector(connector);

    _watcher = new Watcher(_docroot);
    if (_exclusions != null && _exclusions.length > 0) {
      List<Pattern> patterns = new ArrayList<Pattern>();
      for (String exclusion : _exclusions) {
        patterns.add(Pattern.compile(exclusion));
      }
      _watcher.set_patterns(patterns);
    }
    _watcher.listener = wsHandler;

  }

  public static void setExclusions(String[] exclusions) {
    LRServer._exclusions = exclusions;
  }

  public static String[] getExclusions() {
    return _exclusions;
  }

  public void start() throws Exception {
    this.init();
    _server.start();
    _watcher.start();
  }

  public void run() throws Exception {
    try {
      start();
      join();
    } catch (Throwable t) {
      System.err.println("Caught unexpected exception: " + t);
      System.err.println();
      t.printStackTrace(System.err);
    } finally {
      stop();
    }
  }

  public void join() throws Exception {
    _server.join();
  }

  public void stop() throws Exception {
    _watcher.stop();
    _server.stop();
  }
}
