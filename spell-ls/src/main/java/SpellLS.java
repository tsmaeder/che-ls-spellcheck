import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

import com.swabunga.spell.engine.SpellDictionaryHashMap;
import com.swabunga.spell.event.AbstractWordTokenizer;
import com.swabunga.spell.event.SpellCheckEvent;
import com.swabunga.spell.event.SpellCheckListener;
import com.swabunga.spell.event.SpellChecker;

public class SpellLS implements LanguageServer {
	private static class LineIndex {
		private List<Integer> lineStarts;

		public LineIndex(String text) {
			lineStarts = new ArrayList<>();
			lineStarts.add(0);
			for (int i = 0; i < text.length(); i++) {
				if (text.charAt(i) == '\r') {
					if (i < text.length() - 1 && text.charAt(i + 1) == '\n') {
						i++;
					}
					lineStarts.add(i + 1);
				} else if (text.charAt(i) == '\n') {
					lineStarts.add(i + 1);
				}

			}
		}

		public Position convert(int offset) {
			if (offset == 0) {
				return new Position(0, 0);
			}
			int index = Collections.binarySearch(lineStarts, offset);
			if (index < 0) {
				index= -index-2;
			}
			int lineStart = lineStarts.get(index);
			return new Position(index, offset - lineStart);
		}

		public Range convert(int startOffset, int endOffset) {
			return new Range(convert(startOffset), convert(endOffset));
		}
	}

	private LanguageClient client;
	private TextDocumentService textDocumentService = new SpellTextDocumentService(this);
	private SpellChecker spellChecker;

	public static void main(String[] args) throws IOException {
		SpellLS server = new SpellLS();
		Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, System.in, System.out);
		launcher.startListening();
		server.setClient(launcher.getRemoteProxy());
	}

	void setClient(LanguageClient remoteProxy) {
		this.client= remoteProxy;
	}

	public SpellLS() throws IOException {
		InputStream resourceAsStream = SpellLS.class.getClassLoader().getResourceAsStream("english.0");
		this.spellChecker = new SpellChecker(new SpellDictionaryHashMap(new InputStreamReader(resourceAsStream)));
		spellChecker.addSpellCheckListener(new SpellCheckListener() {

			public void spellingError(SpellCheckEvent event) {
				event.replaceWord("foobar", false);
			}
		});
	}

	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		InitializeResult initializeResult = new InitializeResult();
		ServerCapabilities capabilities = new ServerCapabilities();
		capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
		initializeResult.setCapabilities(capabilities);
		return CompletableFuture.completedFuture(initializeResult);
	}

	public CompletableFuture<Object> shutdown() {
		return null;
	}

	public void exit() {
		System.exit(0);
	}

	public TextDocumentService getTextDocumentService() {
		return textDocumentService;
	}

	public WorkspaceService getWorkspaceService() {
		return null;
	}

	public LanguageClient getClient() {
		return client;
	}

	void checkSpelling(String uri, String text) {
		List<Diagnostic> diagnostics = new ArrayList<>();
		LineIndex index = new LineIndex(text);
		spellChecker.checkSpelling(new AbstractWordTokenizer(text) {

			@Override
			public void replaceWord(String newWord) {
				Diagnostic diagnostic = new Diagnostic(
						index.convert(getCurrentWordPosition(), getCurrentWordEnd()),
						String.format("misspelt word: %s", currentWord.getText()),
						DiagnosticSeverity.Error,
						"Spell-LS",
						currentWord.getText());
				diagnostics.add(diagnostic);
			}
		});
		PublishDiagnosticsParams params = new PublishDiagnosticsParams(uri, diagnostics);
		getClient().publishDiagnostics(params);
	}
}
