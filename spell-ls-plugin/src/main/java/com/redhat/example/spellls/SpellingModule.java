package com.redhat.example.spellls;

import static java.util.Arrays.asList;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.eclipse.che.api.languageserver.launcher.LanguageServerLauncher;
import org.eclipse.che.api.languageserver.shared.model.LanguageDescription;
import org.eclipse.che.inject.DynaModule;

/** @author Anatolii Bazko */
@DynaModule
public class SpellingModule extends AbstractModule {

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), LanguageServerLauncher.class)
        .addBinding()
        .to(SpellingLanguageServerLauncher.class);

    LanguageDescription description = new LanguageDescription();
    description.setFileExtensions(asList("txt"));
    description.setLanguageId("textlanguage");
    description.setMimeType("text/text");
    Multibinder.newSetBinder(binder(), LanguageDescription.class)
        .addBinding()
        .toInstance(description);
  }
}
