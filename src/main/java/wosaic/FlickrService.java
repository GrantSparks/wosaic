package wosaic;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.xml.parsers.ParserConfigurationException;

//import org.xml.sax.SAXException;
import wosaic.exceptions.FlickrServiceException;
import wosaic.utilities.FlickrQuery;
import wosaic.utilities.SourcePlugin;

import com.googlecode.flickrjandroid.*;
import com.googlecode.flickrjandroid.photos.*;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import to.sparks.wosaic.AppProperties;

//import com.aetrion.flickr.Flickr;
//import com.aetrion.flickr.FlickrException;
//import com.aetrion.flickr.REST;
//import com.aetrion.flickr.RequestContext;
//import com.aetrion.flickr.photos.Photo;
//import com.aetrion.flickr.photos.PhotoList;
//import com.aetrion.flickr.photos.PhotosInterface;
//import com.aetrion.flickr.photos.SearchParameters;
/**
 * Our interface for retrieving images from Flickr. Each FlickrService object is
 * unique to a specific search string. Queries to Flickr are made asynchronously
 * through the flickrj API
 *
 * @author scott
 */
public class FlickrService extends SourcePlugin {

    /**
     * Number of pictures to query from Flickr by default
     */
    protected final static int DEFAULT_NUM_PICS = 500;

    /**
     * @author swegner Handle the case when the user presses "Cancel" on the
     * options UI
     */
    class CancelAction extends AbstractAction {

        /**
         * Version ID generated by Eclipse
         */
        private static final long serialVersionUID = 5041800227836704158L;

        /**
         * If the user cancels, simply hide our configuration screen.
         *
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
         */
        public void actionPerformed(final ActionEvent e) {
            OptionsDialog.setVisible(false);
        }

    }

    /**
     * @author swegner Handle event when user presses "Ok" on options UI
     */
    class OkAction extends AbstractAction {

        /**
         * Version ID generated by Eclipse
         */
        private static final long serialVersionUID = -5571219156152814455L;

        /**
         * When the user accepts the options screen, validate our parameters,
         * and then set them.
         *
         * @see
         * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
         */
        public void actionPerformed(final ActionEvent arg0) {
            // Figure out how many results to use
            int target = 0;

            try {
                target = Integer.parseInt(NumSearchField.getText());
                setTargetImages(target);
                OptionsDialog.setVisible(false);
            } catch (final Exception e) {
                JOptionPane
                        .showMessageDialog(
                                OptionsPane,
                                "Invalid input.  Please enter a valid number of images to retrieve.",
                                "Invalid Input", JOptionPane.WARNING_MESSAGE);
            }

        }

    }


    /**
     * Number of connection tries before giving up
     */
    private static final int CONNECT_RETRY = 5;

    /**
     * Flag set to determine if we have a valid connection to Flickr
     */
    private static boolean Connected = false;

    /**
     * Our connection to the FlickrAPI
     */
    private static Flickr Flickr = null;

    /**
     * Needed by Flickr API, the "host" to connect to
     */
//	private static final String HOST = "www.flickr.com";
    /**
     * Needed by Flickr API, access to photo API calls
     */
    private static PhotosInterface PhotosInt = null;

    /**
     * Number of images to grab from Flickr in each query
     */
    private static final int PicsPerQuery = 100;

    /**
     * Needed by Flickr API, access to search query calls
     */
    private static RequestContext ReqCon = null;

    /**
     * Needed by Flickr API, low-level network interface
     */
    private static REST Rest = null;

    /**
     * Static constructor-- initialize our Flickr API and connection to Flickr
     */
    static {
        // Connect to flickr
        try {
            FlickrService.Connect();
        } catch (final ParserConfigurationException ex) {
            // TODO: Handle exceptions here
        }
    }

    private static void Connect() throws ParserConfigurationException {

	// Try to connect at most 'CONNECT_RETRY' times before throwing
        // an exception
        int tryNum = 0;
        while (!FlickrService.Connected) {
            try {
                // Initialize
                FlickrService.Rest = new REST();
                FlickrService.Flickr = new Flickr(AppProperties.getFlickrApiKey(), AppProperties.getFlickrSecret());
                FlickrService.ReqCon = RequestContext.getRequestContext();

                // Get our picture service
                FlickrService.PhotosInt = FlickrService.Flickr.getPhotosInterface();
                FlickrService.Connected = true;

            } catch (final ParserConfigurationException ex) {
                if (tryNum >= FlickrService.CONNECT_RETRY) {
                    throw ex;
                }

                // If we're not over out limit, simply try again
                tryNum++;
            }
        }
    }

	// Configuration UI Code
    /**
     * Text field to hold the number of photos to search fo
     */
    protected JTextField NumSearchField = null;

    /**
     * Dialog to present user with configuration options
     */
    protected JDialog OptionsDialog = null;

    /**
     * The JPanel that actually holds the configuration UI elements
     */
    protected JPanel OptionsPane = null;

    private SearchParameters Params = null;

    private int TargetImages;

    /**
     * Create a new FlickrService that will make the under-lying connections to
     * the Flickr API. Note that a new FlickrService should be initialized for
     * each new search query. This no-argument constructor essentially replaces
     * our previous constructor, as it's required for the Sources API.
     *
     * @throws FlickrServiceException If the Flickr API encounters an error
     */
    public FlickrService() throws FlickrServiceException {
        if (!FlickrService.Connected) {
            try {
                FlickrService.Connect();
            } catch (final ParserConfigurationException ex) {
                throw new FlickrServiceException("Cannot connect to Flickr", ex);
            }
        }

        // Set our parameters
        Params = new SearchParameters();
        Params.setSort(SearchParameters.RELEVANCE);

        initOptionsPane();
        setTargetImages(DEFAULT_NUM_PICS);
    }

    /**
     * Return our configuration panel for the user to set parameters
     *
     * @see wosaic.utilities.SourcePlugin#getOptionsDialog()
     */
    @Override
    public JDialog getOptionsDialog() {
        return OptionsDialog;
    }

    /**
     * String needed by the plugins API to distinguish our plugin
     *
     * @see wosaic.utilities.SourcePlugin#getType()
     */
    @Override
    public Sources.Plugin getType() {
        return Sources.Plugin.Flickr;
    }

    /**
     * Create our options pane. Right now we only select the number of pictures
     * to query for.
     */
    public void initOptionsPane() {

        // Number of Search Results
        OptionsPane = new JPanel();
        OptionsPane.setLayout(new GridBagLayout());

        // Label
        final GridBagConstraints numSearchLabelConstraints = new GridBagConstraints();
        numSearchLabelConstraints.gridx = 0;
        numSearchLabelConstraints.gridy = 0;
        numSearchLabelConstraints.anchor = GridBagConstraints.WEST;
        numSearchLabelConstraints.gridwidth = 2;
        final JLabel numSearchLabel = new JLabel();
        numSearchLabel.setText("Number of Search Results to Use");
        OptionsPane.add(numSearchLabel, numSearchLabelConstraints);

        final GridBagConstraints spacerConstraints2 = new GridBagConstraints();
        spacerConstraints2.gridx = 0;
        spacerConstraints2.gridy = 1;
        spacerConstraints2.anchor = GridBagConstraints.WEST;
        final JLabel spacerLabel2 = new JLabel();
        spacerLabel2.setText("      ");
        OptionsPane.add(spacerLabel2, spacerConstraints2);

        // Search Results Field
        NumSearchField = new JTextField(8);
        NumSearchField.setText(String.valueOf(DEFAULT_NUM_PICS));
        final GridBagConstraints numSearchFieldConstraints = new GridBagConstraints();
        numSearchFieldConstraints.gridx = 1;
        numSearchFieldConstraints.gridy = 1;
        numSearchFieldConstraints.anchor = GridBagConstraints.WEST;
        numSearchFieldConstraints.ipadx = 7;
        OptionsPane.add(NumSearchField, numSearchFieldConstraints);

        // Ok Button
        final JButton okButton = new JButton("OK");
        okButton.addActionListener(new OkAction());
        final GridBagConstraints okConstraints = new GridBagConstraints();
        okConstraints.gridx = 0;
        okConstraints.gridy = 2;
        okConstraints.anchor = GridBagConstraints.WEST;
        OptionsPane.add(okButton, okConstraints);

        // Cancel Button
        final JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new CancelAction());
        final GridBagConstraints cancelConstraints = new GridBagConstraints();
        cancelConstraints.gridx = 1;
        cancelConstraints.gridy = 2;
        cancelConstraints.anchor = GridBagConstraints.WEST;
        OptionsPane.add(cancelButton, cancelConstraints);

        OptionsDialog = new JDialog((JFrame) null, "Flickr Options", true);
        OptionsDialog.getContentPane().add(OptionsPane);
        OptionsDialog.pack();
    }

    /**
     * In a new thread, start queuing child threads to query Flickr for results.
     * The results will be saved in SourcesBuffer, and SourcesBuffer.isComplete
     * will be set when it is complete
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        sourcesBuffer.signalProgressCount(TargetImages);

        /*
         * In most cases, our PicsPerQuery won't divide TargetImages, so we'll
         * need to run a partial query. This is especially important for cases
         * where TargetImages < PicsPerQuery.
         */
        final int numPages = TargetImages / FlickrService.PicsPerQuery;
        final int partialQueryPics = TargetImages - numPages
                * FlickrService.PicsPerQuery;
        final int partialPage = (partialQueryPics != 0 ? 1 : 0);

        for (int page = 0; page < numPages + partialPage; page++) {
            PhotoList photos = null;
            int numPics = (page < numPages ? PicsPerQuery : partialQueryPics);

            try {
                photos = PhotosInt.search(Params, numPics, page);
            } catch (final FlickrException ex) {
                System.out.println("FlickrException!");
            } catch (final JSONException ex) {
                System.out.println("JSONException!");
            } catch (final IOException ex) {
                System.out.println("IOException!");
            }

            if (photos == null) {
                System.out.println("Flickr Query failed!");
                continue;
            }

            for (int photoNum = 0; photoNum < photos.size(); photoNum++) {
                final Photo photo = (Photo) photos.get(photoNum);
                ThreadPool.submit(new FlickrQuery(photo.getSmallSquareUrl(),
                        sourcesBuffer));
            }

        }
    }

    /**
     * Publicly-accessible method to set the string to search for
     *
     * @param str string that should be searched.
     */
    @Override
    public void setSearchString(final String str) {
        Params.setText(str);
    }

    /**
     * Publicly accesible function to set how many images to retrieve.
     *
     * @param target The number of images to retrieve.
     */
    public void setTargetImages(final int target) {
        TargetImages = target;
    }

    /**
     * Make sure all of our paramaters have valid values before proceeding.
     *
     * @see wosaic.utilities.SourcePlugin#validateParams()
     */
    @Override
    public String validateParams() {
        if (Params.getText() == null) {
            return "Flickr has an invalid search string!";
        }

        if (TargetImages <= 0) {
            return "Flickr has an invalid number of target images!";
        }

        if (FlickrService.Connected == false) {
            return "Flickr has not connected properly!";
        }

        return null;
    }
}
