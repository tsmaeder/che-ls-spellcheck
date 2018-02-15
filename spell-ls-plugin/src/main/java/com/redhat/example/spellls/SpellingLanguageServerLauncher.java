package com.redhat.example.spellls;

import com.google.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.eclipse.che.api.languageserver.exception.LanguageServerException;
import org.eclipse.che.api.languageserver.launcher.LanguageServerLauncherTemplate;
import org.eclipse.che.api.languageserver.registry.DocumentFilter;
import org.eclipse.che.api.languageserver.registry.LanguageServerDescription;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

public class SpellingLanguageServerLauncher extends LanguageServerLauncherTemplate {

  private static final String REGEX = ".*\\.txt";
  private static final LanguageServerDescription DESCRIPTION = createServerDescription();

  private final Path launchScript = Paths.get(System.getenv("HOME"), "che/ls-spelling/launch.sh");

  @Inject
  public SpellingLanguageServerLauncher() {}

  @Override
  public boolean isAbleToLaunch() {
    return Files.exists(launchScript);
  }

  protected LanguageServer connectToLanguageServer(
      final Process languageServerProcess, LanguageClient client) {
    Launcher<LanguageServer> launcher =
        Launcher.createLauncher(
            client,
            LanguageServer.class,
            languageServerProcess.getInputStream(),
            languageServerProcess.getOutputStream());
    launcher.startListening();
    return launcher.getRemoteProxy();
  }

  protected Process startLanguageServerProcess(String projectPath) throws LanguageServerException {
    ProcessBuilder processBuilder = new ProcessBuilder(launchScript.toString());
    processBuilder.redirectInput(ProcessBuilder.Redirect.PIPE);
    processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);
    try {
      return processBuilder.start();
    } catch (IOException e) {
      throw new LanguageServerException("Can't start JSON language server", e);
    }
  }

  public LanguageServerDescription getDescription() {
    return DESCRIPTION;
  }

  private static LanguageServerDescription createServerDescription() {
    LanguageServerDescription description =
        new LanguageServerDescription(
            "com.redhat.example.spellls",
            null,
            Arrays.asList(new DocumentFilter(null, REGEX, null)));
    return description;
  }
}
