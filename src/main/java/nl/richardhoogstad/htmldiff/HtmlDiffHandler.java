package nl.richardhoogstad.htmldiff;

import org.outerj.daisy.diff.HtmlCleaner;
import org.outerj.daisy.diff.XslFilter;
import org.outerj.daisy.diff.html.HTMLDiffer;
import org.outerj.daisy.diff.html.HtmlSaxDiffOutput;
import org.outerj.daisy.diff.html.TextNodeComparator;
import org.outerj.daisy.diff.html.dom.DomTreeBuilder;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Locale;

public class HtmlDiffHandler implements HtmlDiffService.Iface {

	@Override
	public void createDiffFile(String pageUrl1, String pageUrl2, String outputFileName) {

		boolean htmlOut = true;

		InputStream oldStream = null;
		InputStream newStream = null;

		try {

			File outputFile = new File(outputFileName);
			try {
				outputFile.createNewFile(); // Fail if outputFileName is malformed. Otherwise result.setResult() below would silently supress an exception (at least with jdk1.8.0_65). Then calling postProcess.endDocument() below would fail with confusing "javax.xml.transform.TransformerException: org.xml.sax.SAXException: setResult() must be called prior to startDocument()."
			} catch (IOException e) {
				System.err.println("Filepath " + outputFileName + " is malformed, or some of its folders don't exist, or you don't have write access.");
				return;
			}


			SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory
					.newInstance();

			TransformerHandler result = tf.newTransformerHandler();
			// If the file path were malformed, then the following
			result.setResult(new StreamResult(outputFile));

			oldStream = new URI(pageUrl1).toURL().openStream();
			newStream = new URI(pageUrl2).toURL().openStream();

			XslFilter filter = new XslFilter();


			ContentHandler postProcess = htmlOut ? filter.xsl(result,
					"xslfilter/htmlheader.xsl") : result;

			Locale locale = Locale.getDefault();
			String prefix = "diff";

			HtmlCleaner cleaner = new HtmlCleaner();

			InputSource oldSource = new InputSource(oldStream);
			InputSource newSource = new InputSource(newStream);

			DomTreeBuilder oldHandler = new DomTreeBuilder();
			cleaner.cleanAndParse(oldSource, oldHandler);

			TextNodeComparator leftComparator = new TextNodeComparator(
					oldHandler, locale);

			DomTreeBuilder newHandler = new DomTreeBuilder();
			cleaner.cleanAndParse(newSource, newHandler);

			TextNodeComparator rightComparator = new TextNodeComparator(
					newHandler, locale);

			postProcess.startDocument();
			postProcess.startElement("", "diffreport", "diffreport",
					new AttributesImpl());

			postProcess.startElement("", "diff", "diff",
					new AttributesImpl());
			HtmlSaxDiffOutput output = new HtmlSaxDiffOutput(postProcess,
					prefix);

			HTMLDiffer differ = new HTMLDiffer(output);
			differ.diff(leftComparator, rightComparator);

			postProcess.endElement("", "diff", "diff");
			postProcess.endElement("", "diffreport", "diffreport");
			postProcess.endDocument();


		} catch (Throwable e) {

			e.printStackTrace();
			if (e.getCause() != null) {
				e.getCause().printStackTrace();
			}
			if (e instanceof SAXException) {
				((SAXException) e).getException().printStackTrace();
			}


		} finally {
			try {
				if (oldStream != null) oldStream.close();
			} catch (IOException e) {
				//ignore this exception
			}
			try {
				if (newStream != null) newStream.close();
			} catch (IOException e) {
				//ignore this exception
			}
		}

		System.out.println("done");
	}
}
