package net.sf.jsignpdf;

import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import net.sf.jsignpdf.types.CertificationLevel;
import net.sf.jsignpdf.types.HashAlgorithm;
import net.sf.jsignpdf.types.PrintRight;

/**
 * GUI for PDFSigner.
 * 
 * @author Josef Cacek
 */
public class SignPdfForm extends javax.swing.JFrame implements SignResultListener {

	private static final long serialVersionUID = 1L;

	private SignerFileChooser fc = new SignerFileChooser();

	protected final PropertyProvider props = PropertyProvider.getInstance();
	protected final ResourceProvider res = ResourceProvider.getInstance();

	private PrintWriter infoWriter;
	private TextAreaStream infoStream;
	private boolean autoclose = false;
	private BasicSignerOptions options = new BasicSignerOptions();
	private SignerLogic signerLogic = new SignerLogic(options);
	private VisibleSignatureDialog vsDialog = new VisibleSignatureDialog(this, true, options, fc);
	private TsaDialog tsaDialog = new TsaDialog(this, true, options);

	/** Creates new form SignPdfForm */
	public SignPdfForm(int aCloseOperation) {
		initComponents();
		options.loadOptions();
		translateLabels();

		setDefaultCloseOperation(aCloseOperation);
		getRootPane().setDefaultButton(btnSignIt);

		infoStream = new TextAreaStream(infoTextArea);
		infoWriter = new PrintWriter(infoStream, true);

		// set Icon of frames
		URL tmpImgUrl = getClass().getResource("/net/sf/jsignpdf/signedpdf32.png");
		setIconImage(Toolkit.getDefaultToolkit().getImage(tmpImgUrl));
		infoDialog.setIconImage(getIconImage());
		infoDialog.getRootPane().setDefaultButton(btnInfoClose);
		infoDialog.pack();
		GuiUtils.center(infoDialog);

		// setIconImage is available from Java 1.6!
		// rightsDialog.setIconImage(getIconImage());
		rightsDialog.getRootPane().setDefaultButton(btnRightsOK);
		rightsDialog.pack();
		GuiUtils.center(rightsDialog);

		tsaDialog.pack();
		GuiUtils.center(tsaDialog);

		vsDialog.pack();
		GuiUtils.center(vsDialog);

		options.setPrintWriter(infoWriter);
		options.setListener(this);

		cbKeystoreType.setModel(new DefaultComboBoxModel(KeyStoreUtils.getKeyStores()));
		cbCertLevel.setModel(new DefaultComboBoxModel(CertificationLevel.values()));
		cbHashAlgorithm.setModel(new DefaultComboBoxModel(HashAlgorithm.values()));
		cbPrinting.setModel(new DefaultComboBoxModel(PrintRight.values()));

		updateFromOptions();

		chkbAdvancedActionPerformed(null);
		chkbPdfEncryptedActionPerformed(null);
	}

	/**
	 * Application translations.
	 */
	private void translateLabels() {
		setTitle(res.get("gui.title", new String[] { Constants.VERSION }));
		setLabelAndMnemonic(lblKeystoreType, "gui.keystoreType.label");
		setLabelAndMnemonic(chkbAdvanced, "gui.advancedView.checkbox");
		setLabelAndMnemonic(lblKeystoreFile, "gui.keystoreFile.label");
		setLabelAndMnemonic(lblKeystorePwd, "gui.keystorePassword.label");
		setLabelAndMnemonic(chkbStorePwd, "gui.storePasswords.checkbox");
		setLabelAndMnemonic(lblAlias, "gui.alias.label");
		setLabelAndMnemonic(btnLoadAliases, "gui.loadAliases.button");
		setLabelAndMnemonic(lblKeyPwd, "gui.keyPassword.label");
		setLabelAndMnemonic(lblInPdfFile, "gui.inPdfFile.label");
		setLabelAndMnemonic(chkbPdfEncrypted, "gui.pdfEncrypted.checkbox");
		setLabelAndMnemonic(btnRights, "gui.rights.button");
		setLabelAndMnemonic(lblPdfOwnerPwd, "gui.pdfOwnerPwd.label");
		setLabelAndMnemonic(lblPdfUserPwd, "gui.pdfUserPwd.label");
		setLabelAndMnemonic(lblOutPdfFile, "gui.outPdfFile.label");
		setLabelAndMnemonic(lblReason, "gui.reason.label");
		setLabelAndMnemonic(lblLocation, "gui.location.label");
		setLabelAndMnemonic(lblContact, "gui.contact.label");
		setLabelAndMnemonic(lblCertLevel, "gui.certLevel.label");
		setLabelAndMnemonic(chkbAppendSignature, "gui.appendSignature.checkbox");
		setLabelAndMnemonic(lblHashAlgorithm, "gui.hashAlgorithm.label");

		btnKeystoreFile.setText(res.get("gui.browse.button"));
		btnInPdfFile.setText(res.get("gui.browse.button"));
		btnOutPdfFile.setText(res.get("gui.browse.button"));

		setLabelAndMnemonic(btnSignIt, "gui.signIt.button");

		infoDialog.setTitle(res.get("gui.info.title"));
		btnInfoClose.setText(res.get("gui.info.close.button"));

		rightsDialog.setTitle(res.get("gui.rights.title"));
		setLabelAndMnemonic(lblPrinting, "gui.rights.printing.label");
		setLabelAndMnemonic(lblRights, "gui.rights.rights.label");
		setLabelAndMnemonic(chkbAllowCopy, "gui.rights.copy.checkbox");
		setLabelAndMnemonic(chkbAllowAssembly, "gui.rights.assembly.checkbox");
		setLabelAndMnemonic(chkbAllowFillIn, "gui.rights.fillIn.checkbox");
		setLabelAndMnemonic(chkbAllowScreenReaders, "gui.rights.screenReaders.checkbox");
		setLabelAndMnemonic(chkbAllowModifyAnnotations, "gui.rights.modifyAnnotations.checkbox");
		setLabelAndMnemonic(chkbAllowModifyContent, "gui.rights.modifyContents.checkbox");

		setLabelAndMnemonic(chkbVisibleSig, "gui.visibleSignature.checkbox");
		setLabelAndMnemonic(btnVisibleSigSettings, "gui.visibleSignatureSettings.button");

		setLabelAndMnemonic(btnTsaOcsp, "gui.tsaOcsp.button");
	}

	/**
	 * Sets translations and mnemonics for labels and different kind of buttons
	 * 
	 * @param aComponent
	 *            component in which should be label set
	 * @param aKey
	 *            message key
	 */
	private void setLabelAndMnemonic(final JComponent aComponent, final String aKey) {
		res.setLabelAndMnemonic(aComponent, aKey);
	}

	/**
	 * Loads properties saved by previous run of application
	 */
	private void updateFromOptions() {
		cbKeystoreType.setSelectedItem(options.getKsType());
		chkbAdvanced.setSelected(options.isAdvanced());
		tfKeystoreFile.setText(options.getKsFile());
		pfKeystorePwd.setText(options.getKsPasswdStr());
		chkbStorePwd.setSelected(options.isStorePasswords());
		cbAlias.setSelectedItem(options.getKeyAlias());
		pfKeyPwd.setText(options.getKeyPasswdStr());
		tfInPdfFile.setText(options.getInFile());
		chkbPdfEncrypted.setSelected(options.isEncrypted());
		pfPdfOwnerPwd.setText(options.getPdfOwnerPwdStr());
		pfPdfUserPwd.setText(options.getPdfUserPwdStr());
		tfOutPdfFile.setText(options.getOutFile());
		tfReason.setText(options.getReason());
		tfLocation.setText(options.getLocation());
		tfContact.setText(options.getContact());
		cbCertLevel.setSelectedItem(options.getCertLevel());
		cbHashAlgorithm.setSelectedItem(options.getHashAlgorithm());
		chkbAppendSignature.setSelected(options.isAppend());

		cbPrinting.setSelectedItem(options.getRightPrinting());
		chkbAllowCopy.setSelected(options.isRightCopy());
		chkbAllowAssembly.setSelected(options.isRightAssembly());
		chkbAllowFillIn.setSelected(options.isRightFillIn());
		chkbAllowScreenReaders.setSelected(options.isRightScreanReaders());
		chkbAllowModifyAnnotations.setSelected(options.isRightModifyAnnotations());
		chkbAllowModifyContent.setSelected(options.isRightModifyContents());

		chkbVisibleSig.setSelected(options.isVisible());

		switchAdvancedView(options.isAdvanced());
		switchEncryptedPdf(options.isEncrypted());
		switchVisibleSignature(options.isVisible());
		pack();
	}

	/**
	 * stores values from this Form to the instance of {@link SignerOptions}
	 */
	private void storeToOptions() {
		options.setKsType((String) cbKeystoreType.getSelectedItem());
		options.setAdvanced(chkbAdvanced.isSelected());
		options.setKsFile(tfKeystoreFile.getText());
		options.setKsPasswd(pfKeystorePwd.getPassword());
		options.setStorePasswords(chkbStorePwd.isSelected());
		if (cbAlias.getSelectedItem() != options.getKeyAlias() || cbAlias.getSelectedIndex() > -1) {
			options.setKeyAlias((String) cbAlias.getSelectedItem());
			options.setKeyIndex(cbAlias.getSelectedIndex());
		}
		options.setKeyPasswd(pfKeyPwd.getPassword());
		options.setInFile(tfInPdfFile.getText());
		options.setEncrypted(chkbPdfEncrypted.isSelected());
		options.setPdfOwnerPwd(pfPdfOwnerPwd.getPassword());
		options.setPdfUserPwd(pfPdfUserPwd.getPassword());
		options.setOutFile(tfOutPdfFile.getText());
		options.setReason(tfReason.getText());
		options.setLocation(tfLocation.getText());
		options.setContact(tfContact.getText());
		options.setCertLevel((CertificationLevel) cbCertLevel.getSelectedItem());
		options.setHashAlgorithm((HashAlgorithm) cbHashAlgorithm.getSelectedItem());
		options.setAppend(chkbAppendSignature.isSelected());

		options.setRightPrinting((PrintRight) cbPrinting.getSelectedItem());
		options.setRightCopy(chkbAllowCopy.isSelected());
		options.setRightAssembly(chkbAllowAssembly.isSelected());
		options.setRightFillIn(chkbAllowFillIn.isSelected());
		options.setRightScreanReaders(chkbAllowScreenReaders.isSelected());
		options.setRightModifyAnnotations(chkbAllowModifyAnnotations.isSelected());
		options.setRightModifyContents(chkbAllowModifyContent.isSelected());

		options.setVisible(chkbVisibleSig.isSelected());
	}

	/**
	 * Handles switching Advanced checkbox. Sets some components visible/hidden
	 * depending on given status flag.
	 * 
	 * @param anAdvanced
	 *            flag - advanced view is enabled
	 */
	private void switchAdvancedView(boolean anAdvanced) {
		btnLoadAliases.setVisible(anAdvanced);
		lblAlias.setVisible(anAdvanced);
		cbAlias.setVisible(anAdvanced);
		lblKeyPwd.setVisible(anAdvanced);
		pfKeyPwd.setVisible(anAdvanced);
		chkbPdfEncrypted.setVisible(anAdvanced);
		lblCertLevel.setVisible(anAdvanced);
		cbCertLevel.setVisible(anAdvanced);
		lblHashAlgorithm.setVisible(anAdvanced);
		cbHashAlgorithm.setVisible(anAdvanced);
		chkbAppendSignature.setVisible(anAdvanced);
		btnTsaOcsp.setVisible(anAdvanced);
	}

	/**
	 * Handles switching Visible signature checkbox. Sets button Settings
	 * enabled/disabled
	 * 
	 * @param anVisible
	 *            flag - visible signature is enabled
	 */
	private void switchVisibleSignature(boolean anVisible) {
		btnVisibleSigSettings.setEnabled(anVisible);
	}

	/**
	 * Handles switching Encrypted checkbox. Sets some components
	 * enabled/disabled depending on given status flag.
	 * 
	 * @param anEnabled
	 *            flag - encrypted view is enabled
	 */
	private void switchEncryptedPdf(boolean anEnabled) {
		final boolean tmpEncrypted = chkbAdvanced.isSelected() && anEnabled;
		lblPdfOwnerPwd.setVisible(tmpEncrypted);
		pfPdfOwnerPwd.setVisible(tmpEncrypted);
		lblPdfUserPwd.setVisible(tmpEncrypted);
		pfPdfUserPwd.setVisible(tmpEncrypted);
		btnRights.setVisible(tmpEncrypted);

		chkbAppendSignature.setEnabled(!anEnabled);
	}

	/**
	 * Displays file chooser dialog of given type and with givet FileFilter.
	 * 
	 * @param aFileField
	 *            assigned textfield
	 * @param aFilter
	 *            filefilter
	 * @param aType
	 *            dialog type (SAVE_DIALOG, OPEN_DIALOG)
	 */
	void showFileChooser(final JTextField aFileField, final FileFilter aFilter, final int aType) {
		fc.showFileChooser(aFileField, aFilter, aType);
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed"
	// <editor-fold defaultstate="collapsed"
	// desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		infoDialog = new javax.swing.JFrame();
		infoScrollPane = new javax.swing.JScrollPane();
		infoTextArea = new javax.swing.JTextArea();
		btnInfoClose = new javax.swing.JButton();
		rightsDialog = new javax.swing.JDialog();
		lblPrinting = new javax.swing.JLabel();
		cbPrinting = new javax.swing.JComboBox();
		lblRights = new javax.swing.JLabel();
		chkbAllowCopy = new javax.swing.JCheckBox();
		chkbAllowAssembly = new javax.swing.JCheckBox();
		chkbAllowFillIn = new javax.swing.JCheckBox();
		chkbAllowScreenReaders = new javax.swing.JCheckBox();
		chkbAllowModifyAnnotations = new javax.swing.JCheckBox();
		chkbAllowModifyContent = new javax.swing.JCheckBox();
		btnRightsOK = new javax.swing.JButton();
		lblKeystoreType = new javax.swing.JLabel();
		cbKeystoreType = new javax.swing.JComboBox();
		lblKeystoreFile = new javax.swing.JLabel();
		btnVisibleSigSettings = new javax.swing.JButton();
		tfKeystoreFile = new javax.swing.JTextField();
		btnKeystoreFile = new javax.swing.JButton();
		lblKeystorePwd = new javax.swing.JLabel();
		pfKeystorePwd = new javax.swing.JPasswordField();
		chkbStorePwd = new javax.swing.JCheckBox();
		lblAlias = new javax.swing.JLabel();
		cbAlias = new javax.swing.JComboBox();
		btnLoadAliases = new javax.swing.JButton();
		lblKeyPwd = new javax.swing.JLabel();
		pfKeyPwd = new javax.swing.JPasswordField();
		lblInPdfFile = new javax.swing.JLabel();
		tfInPdfFile = new javax.swing.JTextField();
		btnInPdfFile = new javax.swing.JButton();
		chkbPdfEncrypted = new javax.swing.JCheckBox();
		lblPdfOwnerPwd = new javax.swing.JLabel();
		pfPdfOwnerPwd = new javax.swing.JPasswordField();
		lblPdfUserPwd = new javax.swing.JLabel();
		pfPdfUserPwd = new javax.swing.JPasswordField();
		lblOutPdfFile = new javax.swing.JLabel();
		tfOutPdfFile = new javax.swing.JTextField();
		btnOutPdfFile = new javax.swing.JButton();
		lblReason = new javax.swing.JLabel();
		tfReason = new javax.swing.JTextField();
		lblLocation = new javax.swing.JLabel();
		tfLocation = new javax.swing.JTextField();
		lblCertLevel = new javax.swing.JLabel();
		cbCertLevel = new javax.swing.JComboBox();
		chkbAppendSignature = new javax.swing.JCheckBox();
		btnSignIt = new javax.swing.JButton();
		chkbAdvanced = new javax.swing.JCheckBox();
		btnRights = new javax.swing.JButton();
		chkbVisibleSig = new javax.swing.JCheckBox();
		lblContact = new javax.swing.JLabel();
		tfContact = new javax.swing.JTextField();
		btnTsaOcsp = new javax.swing.JButton();
		cbHashAlgorithm = new javax.swing.JComboBox();
		lblHashAlgorithm = new javax.swing.JLabel();

		infoDialog.setTitle("PDF Signer Output");
		infoDialog.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				infoDialogWindowClosing(evt);
			}
		});

		infoTextArea.setColumns(80);
		infoTextArea.setEditable(false);
		infoTextArea.setFont(new java.awt.Font("Courier New", 1, 10));
		infoTextArea.setRows(25);
		infoTextArea.setMinimumSize(new java.awt.Dimension(200, 180));
		infoScrollPane.setViewportView(infoTextArea);

		infoDialog.getContentPane().add(infoScrollPane, java.awt.BorderLayout.CENTER);

		btnInfoClose.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/jsignpdf/back16.png"))); // NOI18N
		btnInfoClose.setText("Close");
		btnInfoClose.setMinimumSize(new java.awt.Dimension(50, 20));
		btnInfoClose.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnInfoCloseActionPerformed(evt);
			}
		});
		infoDialog.getContentPane().add(btnInfoClose, java.awt.BorderLayout.SOUTH);

		rightsDialog.setModal(true);
		rightsDialog.getContentPane().setLayout(new java.awt.GridBagLayout());

		lblPrinting.setText("Printing");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
		rightsDialog.getContentPane().add(lblPrinting, gridBagConstraints);

		cbPrinting
				.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Allow", "Allow Degraded", "Disallow" }));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
		rightsDialog.getContentPane().add(cbPrinting, gridBagConstraints);

		lblRights.setText("Rights");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
		rightsDialog.getContentPane().add(lblRights, gridBagConstraints);

		chkbAllowCopy.setText("Copy");
		chkbAllowCopy.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
		chkbAllowCopy.setMargin(new java.awt.Insets(0, 0, 0, 0));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
		rightsDialog.getContentPane().add(chkbAllowCopy, gridBagConstraints);

		chkbAllowAssembly.setText("Assembly");
		chkbAllowAssembly.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
		chkbAllowAssembly.setMargin(new java.awt.Insets(0, 0, 0, 0));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
		rightsDialog.getContentPane().add(chkbAllowAssembly, gridBagConstraints);

		chkbAllowFillIn.setText("Fill In");
		chkbAllowFillIn.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
		chkbAllowFillIn.setMargin(new java.awt.Insets(0, 0, 0, 0));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
		rightsDialog.getContentPane().add(chkbAllowFillIn, gridBagConstraints);

		chkbAllowScreenReaders.setText("Screen readers");
		chkbAllowScreenReaders.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
		chkbAllowScreenReaders.setMargin(new java.awt.Insets(0, 0, 0, 0));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
		rightsDialog.getContentPane().add(chkbAllowScreenReaders, gridBagConstraints);

		chkbAllowModifyAnnotations.setText("Modify annotations");
		chkbAllowModifyAnnotations.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
		chkbAllowModifyAnnotations.setMargin(new java.awt.Insets(0, 0, 0, 0));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
		rightsDialog.getContentPane().add(chkbAllowModifyAnnotations, gridBagConstraints);

		chkbAllowModifyContent.setText("Modify contents");
		chkbAllowModifyContent.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
		chkbAllowModifyContent.setMargin(new java.awt.Insets(0, 0, 0, 0));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
		rightsDialog.getContentPane().add(chkbAllowModifyContent, gridBagConstraints);

		btnRightsOK.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/jsignpdf/back16.png"))); // NOI18N
		btnRightsOK.setText("OK");
		btnRightsOK.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnRightsOKActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 5;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(7, 5, 2, 5);
		rightsDialog.getContentPane().add(btnRightsOK, gridBagConstraints);

		setTitle("SignPdf");
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				formWindowClosing(evt);
			}
		});
		getContentPane().setLayout(new java.awt.GridBagLayout());

		lblKeystoreType.setLabelFor(cbKeystoreType);
		lblKeystoreType.setText("Keystore type");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblKeystoreType, gridBagConstraints);

		cbKeystoreType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "PKCS#12", "JKS" }));
		cbKeystoreType.setMinimumSize(new java.awt.Dimension(150, 20));
		cbKeystoreType.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(cbKeystoreType, gridBagConstraints);

		lblKeystoreFile.setLabelFor(tfKeystoreFile);
		lblKeystoreFile.setText("Keystore file");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblKeystoreFile, gridBagConstraints);

		btnVisibleSigSettings.setIcon(new javax.swing.ImageIcon(getClass()
				.getResource("/net/sf/jsignpdf/options16.png"))); // NOI18N
		btnVisibleSigSettings.setText("Settings");
		btnVisibleSigSettings.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
		btnVisibleSigSettings.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnVisibleSigSettingsActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 15;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
		getContentPane().add(btnVisibleSigSettings, gridBagConstraints);

		tfKeystoreFile.setMinimumSize(new java.awt.Dimension(250, 20));
		tfKeystoreFile.setPreferredSize(new java.awt.Dimension(250, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.weightx = 4.0;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(tfKeystoreFile, gridBagConstraints);

		btnKeystoreFile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/jsignpdf/fileopen16.png"))); // NOI18N
		btnKeystoreFile.setText("Browse...");
		btnKeystoreFile.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
		btnKeystoreFile.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnKeystoreFileActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
		getContentPane().add(btnKeystoreFile, gridBagConstraints);

		lblKeystorePwd.setLabelFor(pfKeystorePwd);
		lblKeystorePwd.setText("Keystore password");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblKeystorePwd, gridBagConstraints);

		pfKeystorePwd.setMinimumSize(new java.awt.Dimension(150, 20));
		pfKeystorePwd.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(pfKeystorePwd, gridBagConstraints);

		chkbStorePwd.setText("Store passwords");
		chkbStorePwd.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
		chkbStorePwd.setMargin(new java.awt.Insets(0, 0, 0, 0));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
		getContentPane().add(chkbStorePwd, gridBagConstraints);

		lblAlias.setLabelFor(cbAlias);
		lblAlias.setText("Key alias");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblAlias, gridBagConstraints);

		cbAlias.setEditable(true);
		cbAlias.setMinimumSize(new java.awt.Dimension(150, 20));
		cbAlias.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(cbAlias, gridBagConstraints);

		btnLoadAliases.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/jsignpdf/refresh16.png"))); // NOI18N
		btnLoadAliases.setText("Load keys");
		btnLoadAliases.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
		btnLoadAliases.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnLoadAliasesActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
		getContentPane().add(btnLoadAliases, gridBagConstraints);

		lblKeyPwd.setLabelFor(pfKeyPwd);
		lblKeyPwd.setText("Key password");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblKeyPwd, gridBagConstraints);

		pfKeyPwd.setMinimumSize(new java.awt.Dimension(150, 20));
		pfKeyPwd.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(pfKeyPwd, gridBagConstraints);

		lblInPdfFile.setLabelFor(tfInPdfFile);
		lblInPdfFile.setText("Input PDF file");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 5;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblInPdfFile, gridBagConstraints);

		tfInPdfFile.setMinimumSize(new java.awt.Dimension(150, 20));
		tfInPdfFile.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 5;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(tfInPdfFile, gridBagConstraints);

		btnInPdfFile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/jsignpdf/fileopen16.png"))); // NOI18N
		btnInPdfFile.setText("Browse...");
		btnInPdfFile.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
		btnInPdfFile.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnInPdfFileActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 5;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
		getContentPane().add(btnInPdfFile, gridBagConstraints);

		chkbPdfEncrypted.setText("Encrypted");
		chkbPdfEncrypted.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
		chkbPdfEncrypted.setMargin(new java.awt.Insets(0, 0, 0, 0));
		chkbPdfEncrypted.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				chkbPdfEncryptedActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 6;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(chkbPdfEncrypted, gridBagConstraints);

		lblPdfOwnerPwd.setLabelFor(pfPdfOwnerPwd);
		lblPdfOwnerPwd.setText("Owner password");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 7;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblPdfOwnerPwd, gridBagConstraints);

		pfPdfOwnerPwd.setMinimumSize(new java.awt.Dimension(150, 20));
		pfPdfOwnerPwd.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 7;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(pfPdfOwnerPwd, gridBagConstraints);

		lblPdfUserPwd.setLabelFor(pfPdfUserPwd);
		lblPdfUserPwd.setText("User password");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 8;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblPdfUserPwd, gridBagConstraints);

		pfPdfUserPwd.setMinimumSize(new java.awt.Dimension(150, 20));
		pfPdfUserPwd.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 8;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(pfPdfUserPwd, gridBagConstraints);

		lblOutPdfFile.setLabelFor(tfOutPdfFile);
		lblOutPdfFile.setText("Output PDF file");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 9;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblOutPdfFile, gridBagConstraints);

		tfOutPdfFile.setMinimumSize(new java.awt.Dimension(150, 20));
		tfOutPdfFile.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 9;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(tfOutPdfFile, gridBagConstraints);

		btnOutPdfFile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/jsignpdf/fileopen16.png"))); // NOI18N
		btnOutPdfFile.setText("Browse...");
		btnOutPdfFile.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
		btnOutPdfFile.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnOutPdfFileActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 9;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
		getContentPane().add(btnOutPdfFile, gridBagConstraints);

		lblReason.setLabelFor(tfReason);
		lblReason.setText("Reason");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 10;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblReason, gridBagConstraints);

		tfReason.setMinimumSize(new java.awt.Dimension(150, 20));
		tfReason.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 10;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(tfReason, gridBagConstraints);

		lblLocation.setLabelFor(tfLocation);
		lblLocation.setText("Location");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 11;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblLocation, gridBagConstraints);

		tfLocation.setMinimumSize(new java.awt.Dimension(150, 20));
		tfLocation.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 11;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(tfLocation, gridBagConstraints);

		lblCertLevel.setLabelFor(cbCertLevel);
		lblCertLevel.setText("Certification level");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 13;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblCertLevel, gridBagConstraints);

		cbCertLevel.setMinimumSize(new java.awt.Dimension(150, 20));
		cbCertLevel.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 13;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(cbCertLevel, gridBagConstraints);

		chkbAppendSignature.setText("Append signature");
		chkbAppendSignature.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
		chkbAppendSignature.setMargin(new java.awt.Insets(0, 0, 0, 0));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 13;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
		getContentPane().add(chkbAppendSignature, gridBagConstraints);

		btnSignIt.setFont(new java.awt.Font("Tahoma", 1, 12));
		btnSignIt.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/jsignpdf/signedpdf26.png"))); // NOI18N
		btnSignIt.setText("Sign It");
		btnSignIt.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnSignItActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 16;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		getContentPane().add(btnSignIt, gridBagConstraints);

		chkbAdvanced.setText("Advanced view");
		chkbAdvanced.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
		chkbAdvanced.setMargin(new java.awt.Insets(0, 0, 0, 0));
		chkbAdvanced.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				chkbAdvancedActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
		getContentPane().add(chkbAdvanced, gridBagConstraints);

		btnRights.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/jsignpdf/security16.png"))); // NOI18N
		btnRights.setText("Rights");
		btnRights.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
		btnRights.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnRightsActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 6;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
		getContentPane().add(btnRights, gridBagConstraints);

		chkbVisibleSig.setText("Visible signature");
		chkbVisibleSig.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
		chkbVisibleSig.setMargin(new java.awt.Insets(0, 0, 0, 0));
		chkbVisibleSig.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				chkbVisibleSigActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 15;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(chkbVisibleSig, gridBagConstraints);

		lblContact.setLabelFor(tfContact);
		lblContact.setText("Contact");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 12;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblContact, gridBagConstraints);

		tfContact.setMinimumSize(new java.awt.Dimension(150, 20));
		tfContact.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 12;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(tfContact, gridBagConstraints);

		btnTsaOcsp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/jsignpdf/clock16.png"))); // NOI18N
		btnTsaOcsp.setText("TSA & OCSP");
		btnTsaOcsp.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
		btnTsaOcsp.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnTsaOcspActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 12;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
		getContentPane().add(btnTsaOcsp, gridBagConstraints);

		cbHashAlgorithm.setMinimumSize(new java.awt.Dimension(150, 20));
		cbHashAlgorithm.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 14;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(cbHashAlgorithm, gridBagConstraints);

		lblHashAlgorithm.setLabelFor(cbHashAlgorithm);
		lblHashAlgorithm.setText("Hash algorithm");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 14;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblHashAlgorithm, gridBagConstraints);

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void btnVisibleSigSettingsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnVisibleSigSettingsActionPerformed
		storeToOptions();
		vsDialog.setVisible(true);
	}// GEN-LAST:event_btnVisibleSigSettingsActionPerformed

	private void chkbVisibleSigActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_chkbVisibleSigActionPerformed
		switchVisibleSignature(chkbVisibleSig.isSelected());
	}// GEN-LAST:event_chkbVisibleSigActionPerformed

	private void btnRightsOKActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnRightsOKActionPerformed
		rightsDialog.setVisible(false);
	}// GEN-LAST:event_btnRightsOKActionPerformed

	private void btnRightsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnRightsActionPerformed
		rightsDialog.setVisible(true);
	}// GEN-LAST:event_btnRightsActionPerformed

	private void btnLoadAliasesActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnLoadAliasesActionPerformed
		storeToOptions();
		try {
			cbAlias.setModel(new DefaultComboBoxModel(KeyStoreUtils.getKeyAliases(options)));
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, e.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}// GEN-LAST:event_btnLoadAliasesActionPerformed

	private void chkbPdfEncryptedActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_chkbPdfEncryptedActionPerformed
		switchEncryptedPdf(chkbPdfEncrypted.isSelected());
		pack();
	}// GEN-LAST:event_chkbPdfEncryptedActionPerformed

	private void chkbAdvancedActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_chkbAdvancedActionPerformed
		switchAdvancedView(chkbAdvanced.isSelected());
		switchEncryptedPdf(chkbPdfEncrypted.isSelected());
		pack();
	}// GEN-LAST:event_chkbAdvancedActionPerformed

	private void infoDialogWindowClosing(java.awt.event.WindowEvent evt) {// GEN-FIRST:event_infoDialogWindowClosing
		if (btnInfoClose.isEnabled()) {
			setVisible(true);
		}
	}// GEN-LAST:event_infoDialogWindowClosing

	/**
	 * @see net.sf.jsignpdf.SignResultListener#signerFinishedEvent(boolean)
	 */
	public synchronized void signerFinishedEvent(boolean success) {
		btnInfoClose.setEnabled(true);
		infoDialog.setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);
		if (!btnInfoClose.hasFocus())
			btnInfoClose.requestFocus();
		if (autoclose)
			btnInfoCloseActionPerformed(null);
	}

	private void btnInfoCloseActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnInfoCloseActionPerformed
		infoDialog.setVisible(false);
		setVisible(true);
	}// GEN-LAST:event_btnInfoCloseActionPerformed

	/**
	 * Checks if file exists and it's possible write to it.
	 * 
	 * @param aTF
	 *            text field with file name filled
	 * @param aFileDescKey
	 *            file description (used in error message)
	 * @return result of the check
	 */
	private boolean checkFileExists(JTextField aTF, String aFileDescKey) {
		final String tmpFileName = aTF.getText();
		try {
			if (tmpFileName != null) {
				File tmpFile = new File(tmpFileName);
				if (tmpFile.canRead() && !tmpFile.isDirectory()) {
					return true;
				}
			}
		} catch (Exception e) {
		}

		final String tmpMsg = res.get("gui.fileNotExists.error", new String[] { res.get(aFileDescKey) });
		JOptionPane.showMessageDialog(this, tmpMsg, res.get("gui.check.error.title"), JOptionPane.ERROR_MESSAGE);
		return false;
	}

	/**
	 * Checks if inFile and outFile are different.
	 * 
	 * @return result of the check
	 */
	private boolean checkInOutDiffers() {
		final String tmpInName = tfInPdfFile.getText();
		final String tmpOutName = tfOutPdfFile.getText();
		boolean tmpResult = true;
		if (tmpInName != null && StringUtils.hasLength(tmpOutName)) {
			try {
				final File tmpInFile = (new File(tmpInName)).getAbsoluteFile();
				final File tmpOutFile = (new File(tmpOutName)).getAbsoluteFile();
				if (tmpInFile.equals(tmpOutFile)) {
					tmpResult = false;
					JOptionPane.showMessageDialog(this, res.get("gui.filesEqual.error"), res
							.get("gui.check.error.title"), JOptionPane.ERROR_MESSAGE);
				}
			} catch (Exception e) {
				tmpResult = false;
				JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		return tmpResult;
	}

	/**
	 * Handles pressing of "Sign It" button. Creates and runs SignerLogic
	 * instance in a new thread.
	 * 
	 * @param evt
	 *            event
	 */
	private void btnSignItActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnSignItActionPerformed
		storeToOptions();
		if (checkFileExists(tfInPdfFile, "gui.inPdfFile.label") && checkInOutDiffers()) {
			infoStream.clear();
			btnInfoClose.setEnabled(false);
			infoDialog.setVisible(true);
			setVisible(false);
			infoDialog.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
			infoWriter.println(res.get("console.starting"));
			// Let's do it
			final Thread tmpST = new Thread(signerLogic);
			tmpST.start();
		}
	}// GEN-LAST:event_btnSignItActionPerformed

	private void formWindowClosing(java.awt.event.WindowEvent evt) {// GEN-FIRST:event_formWindowClosing
		storeToOptions();
		options.storeOptions();
	}// GEN-LAST:event_formWindowClosing

	private void btnOutPdfFileActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnOutPdfFileActionPerformed
		showFileChooser(tfOutPdfFile, SignerFileChooser.FILEFILTER_PDF, JFileChooser.SAVE_DIALOG);

	}// GEN-LAST:event_btnOutPdfFileActionPerformed

	private void btnInPdfFileActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnInPdfFileActionPerformed
		showFileChooser(tfInPdfFile, SignerFileChooser.FILEFILTER_PDF, JFileChooser.OPEN_DIALOG);
	}// GEN-LAST:event_btnInPdfFileActionPerformed

	private void btnKeystoreFileActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnKeystoreFileActionPerformed
		showFileChooser(tfKeystoreFile, null, JFileChooser.OPEN_DIALOG);
	}// GEN-LAST:event_btnKeystoreFileActionPerformed

	private void btnTsaOcspActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnTsaOcspActionPerformed
		tsaDialog.setVisible(true);
	}// GEN-LAST:event_btnTsaOcspActionPerformed

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton btnInPdfFile;
	private javax.swing.JButton btnInfoClose;
	private javax.swing.JButton btnKeystoreFile;
	private javax.swing.JButton btnLoadAliases;
	private javax.swing.JButton btnOutPdfFile;
	private javax.swing.JButton btnRights;
	private javax.swing.JButton btnRightsOK;
	private javax.swing.JButton btnSignIt;
	private javax.swing.JButton btnTsaOcsp;
	private javax.swing.JButton btnVisibleSigSettings;
	private javax.swing.JComboBox cbAlias;
	private javax.swing.JComboBox cbCertLevel;
	private javax.swing.JComboBox cbHashAlgorithm;
	private javax.swing.JComboBox cbKeystoreType;
	private javax.swing.JComboBox cbPrinting;
	private javax.swing.JCheckBox chkbAdvanced;
	private javax.swing.JCheckBox chkbAllowAssembly;
	private javax.swing.JCheckBox chkbAllowCopy;
	private javax.swing.JCheckBox chkbAllowFillIn;
	private javax.swing.JCheckBox chkbAllowModifyAnnotations;
	private javax.swing.JCheckBox chkbAllowModifyContent;
	private javax.swing.JCheckBox chkbAllowScreenReaders;
	private javax.swing.JCheckBox chkbAppendSignature;
	private javax.swing.JCheckBox chkbPdfEncrypted;
	private javax.swing.JCheckBox chkbStorePwd;
	private javax.swing.JCheckBox chkbVisibleSig;
	private javax.swing.JFrame infoDialog;
	private javax.swing.JScrollPane infoScrollPane;
	private javax.swing.JTextArea infoTextArea;
	private javax.swing.JLabel lblAlias;
	private javax.swing.JLabel lblCertLevel;
	private javax.swing.JLabel lblContact;
	private javax.swing.JLabel lblHashAlgorithm;
	private javax.swing.JLabel lblInPdfFile;
	private javax.swing.JLabel lblKeyPwd;
	private javax.swing.JLabel lblKeystoreFile;
	private javax.swing.JLabel lblKeystorePwd;
	private javax.swing.JLabel lblKeystoreType;
	private javax.swing.JLabel lblLocation;
	private javax.swing.JLabel lblOutPdfFile;
	private javax.swing.JLabel lblPdfOwnerPwd;
	private javax.swing.JLabel lblPdfUserPwd;
	private javax.swing.JLabel lblPrinting;
	private javax.swing.JLabel lblReason;
	private javax.swing.JLabel lblRights;
	private javax.swing.JPasswordField pfKeyPwd;
	private javax.swing.JPasswordField pfKeystorePwd;
	private javax.swing.JPasswordField pfPdfOwnerPwd;
	private javax.swing.JPasswordField pfPdfUserPwd;
	private javax.swing.JDialog rightsDialog;
	private javax.swing.JTextField tfContact;
	private javax.swing.JTextField tfInPdfFile;
	private javax.swing.JTextField tfKeystoreFile;
	private javax.swing.JTextField tfLocation;
	private javax.swing.JTextField tfOutPdfFile;
	private javax.swing.JTextField tfReason;
	// End of variables declaration//GEN-END:variables

}

/**
 * OutputStream wrapper for writing to TextArea component
 * 
 * @author Josef Cacek
 */
class TextAreaStream extends OutputStream {
	protected final JTextArea textArea;
	protected final ByteArrayOutputStream baos;

	public TextAreaStream(JTextArea textArea) {
		this.textArea = textArea;
		this.baos = new ByteArrayOutputStream();
	}

	public void write(int c) {
		synchronized (this) {
			this.baos.write((char) c);
			this.update();
		}
	}

	public void write(byte[] bytes, int offset, int length) {
		synchronized (this) {
			this.baos.write(bytes, offset, length);
			this.update();
		}
	}

	private void update() {
		String text = new String(this.baos.toByteArray());
		this.textArea.setText(text);
		this.textArea.setCaretPosition(text.length());
	}

	public void clear() {
		synchronized (this) {
			baos.reset();
			update();
		}
	}
}
