import java.util.Collections;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class TestDiagnostics {
	private LanguageClient client;
	private SpellLS spellLS;

	@Before
	public void setUp() throws Exception {
		client = Mockito.mock(LanguageClient.class);
		spellLS = new SpellLS();
		spellLS.setClient(client);
	}

	@Test
	public void testMultiLine() {
		spellLS.getTextDocumentService().didOpen(
				new DidOpenTextDocumentParams(new TextDocumentItem("foouri", "bla", 0, "this\ris\ntekst\r\nyes")));
		Mockito.verify(client)
				.publishDiagnostics(ArgumentMatchers.eq(new PublishDiagnosticsParams("foouri",
						Collections.singletonList(new Diagnostic(new Range(new Position(2, 0), new Position(2, 5)),
								"misspelt word: tekst", DiagnosticSeverity.Error, "Spell-LS", "tekst")))));
	}
}
