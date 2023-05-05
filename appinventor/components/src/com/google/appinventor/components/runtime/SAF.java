package com.google.appinventor.components.runtime;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.YailList;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Non-visible component to access files using Storage Access Framework.
 * Since it is getting difficult with newer Android versions to read and write files using java.io.File() so why not use the
 * not-so popular quite old method (completely) introduced in API 21 (Lollipop) named `Storage Access Framework` to escape
 * from the chaos increasing with time and newer Android versions.
 * Usually, `Application Specific Directory` and app's Private Dir is enough for normal apps which don't have to do much things
 * with files but with SAF we have following benefits -
 * - access phone storage
 * - access SD card
 * - read from/write to files created by other apps
 * - help user in protecting his/her data and restrict Storage consumed by apps
 * - get access to files and folders which are important to your app.
 * In this way, you needn't ask for MANAGE_EXTERNAL_STORAGE permission.
 * - work with non-media files too, such as creating a CSV file of user's data or rendering a PDF file
 */

@DesignerComponent(version = YaVersion.SAF_COMPONENT_VERSION,
        description = "Non-visible component to access files using Storage Access Framework.",
        category = ComponentCategory.STORAGE,
        nonVisible = true,
        androidMinSdk = 21,
        iconName = "images/saf.png")
@SimpleObject
public class SAF extends AndroidNonvisibleComponent implements ActivityResultListener {
    private final Activity activity;
    private final ContentResolver contentResolver;
    private int intentReqCode = 0;

    public SAF(ComponentContainer container) {
        super(container.$form());
        activity = container.$context();
        contentResolver = activity.getContentResolver();
    }

    @Override
    public void resultReturned(int requestCode, int resultCode, Intent intent) {
        if (intentReqCode == requestCode) {
            if (resultCode == Activity.RESULT_OK){
                GotUri(intent.getData(),String.valueOf(intent.getData()));
            }else if (resultCode == Activity.RESULT_CANCELED){
                GotUri("","");
            }
        }
    }

    private int getIntentReqCode() {
        if (intentReqCode == 0) {
            this.intentReqCode = form.registerForActivityResult(this);
        }
        return intentReqCode;
    }

    private void postError(final String method, final String message) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ErrorOccurred(method, message);
            }
        });
    }

    /**
     * Event indicating an error or exception has occurred
     *
     * @param methodName   name of the origin method
     * @param errorMessage text representation of error
     */
    @SimpleEvent(description = "Event indicating error/exception has occurred and returns origin method and error message.")
    public void ErrorOccurred(String methodName, String errorMessage) {
        EventDispatcher.dispatchEvent(this, "ErrorOccurred", methodName, errorMessage);
    }

    @SimpleProperty(description = "Returns mime type of document dir.")
    public String DocumentDirMimeType() {
        return DocumentsContract.Document.MIME_TYPE_DIR;
    }

    @SimpleProperty(description = "Flag to get write permission.")
    public int FlagGrantReadPermission() {
        return Intent.FLAG_GRANT_READ_URI_PERMISSION;
    }

    @SimpleProperty(description = "Flag to get read permission.")
    public int FlagGrantWritePermission() {
        return Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
    }

    /**
     * Returns resulting flag created from given two flags
     *
     * @param f1 the first flag
     * @param f2 the second flag
     */
    @SimpleFunction(description = "Combines two flags and returns resulting flag.")
    public int CreateFlag(int f1, int f2) {
        return f1 | f2;
    }

    /**
     * Returns text representation of URI
     *
     * @param uri the URI object
     */
    @SimpleFunction(description = "Converts uri to string.")
    public String NormalizeUri(Object uri) {
        return String.valueOf(uri);
    }

    /**
     * Parses uriString to create URI
     *
     * @param uriString the string to parse
     */
    @SimpleFunction(description = "Creates Uri from string.")
    public Object CreateUri(String uriString) {
        return Uri.parse(uriString);
    }

    /**
     * Prompts user to select a dir (document tree) which can be accessed later along with its children
     *
     * @param title      optional title which will be shown to user
     * @param initialDir optional dir which will be shown to user when file chooser is visible
     */
    @SimpleFunction(description = "Prompts user to select a document tree.")
    public void OpenDocumentTree(String title, String initialDir) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        if (!initialDir.isEmpty()) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(initialDir));
        }
        activity.startActivityForResult(Intent.createChooser(intent, title), getIntentReqCode());
    }

    /**
     * Prompts user to select a single document
     *
     * @param title          optional title which will be shown to user
     * @param type           parent mime type of acceptable files (such as `application/**`)
     * @param extraMimeTypes optional list of acceptable mime types which are child of parent mime type (such as `application/pdf`)
     */
    @SimpleFunction(description = "Prompts user to select a single document.")
    public void OpenSingleDocument(String title, String type, YailList extraMimeTypes) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (!type.isEmpty()) {
            intent.setType(type);
        }
        intent.setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        if (!extraMimeTypes.isEmpty()) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, extraMimeTypes.toStringArray());
        }
        activity.startActivityForResult(Intent.createChooser(intent, title), getIntentReqCode());
    }

    /**
     * Requests Document Providers to grant persistable URI permission to the uri so that application can access it whenever needed as
     * long as file exists or permission is not invoked
     *
     * @param uri      the URI object for which permission will be requested
     * @param modeFlag the flag which will be persisted (such as read or write or maybe both)
     */
    @SimpleFunction(description = "Take a persistable URI permission grant that has been offered. Once taken, the permission grant will be remembered across device reboots.")
    public void TakePersistableUriPermission(Object uri, int modeFlag) {
        activity.getContentResolver().takePersistableUriPermission((Uri) uri, modeFlag);
    }

    /**
     * Returns whether provided uri is a tree uri or not
     *
     * @param uriString the uri string which will be checked
     */
    @SimpleFunction(description = "Returns whether given uri is a tree uri.")
    public boolean IsTreeUri(String uriString) {
        return DocumentsContract.isTreeUri(Uri.parse(uriString));
    }

    /**
     * Returns whether provided uri is of a document or not
     *
     * @param uriString the uri string which will be checked
     */
    @SimpleFunction(description = "Returns whether given uri is a document uri.")
    public boolean IsDocumentUri(String uriString) {
        return DocumentsContract.isDocumentUri(activity, Uri.parse(uriString));
    }

    /**
     * Returns whether provided second uri is child document/dir of first uri or not
     *
     * @param parentUri the parent's uri string
     * @param childUri  the child's uri string
     */
    @SimpleFunction(description = "Returns whether second uri is child of first uri.")
    public boolean IsChildDocumentUri(String parentUri, String childUri) {
        try {
            return DocumentsContract.isChildDocument(activity.getContentResolver(),
                    Uri.parse(parentUri),
                    Uri.parse(childUri));
        } catch (FileNotFoundException e) {
            postError("IsChildDocumentUri", e.getMessage());
        }
        return false;
    }

    /**
     * Returns document id of tree uri
     *
     * @param uriString the tree document's uri
     */
    @SimpleFunction(description = "Returns document id of tree uri.")
    public String GetTreeDocumentId(String uriString) {
        return DocumentsContract.getTreeDocumentId(Uri.parse(uriString));
    }

    /**
     * Returns document id of an uri
     *
     * @param uriString the document's uri
     */
    @SimpleFunction(description = "Returns document id of an uri.")
    public String GetDocumentId(String uriString) {
        return DocumentsContract.getDocumentId(Uri.parse(uriString));
    }

    /**
     * Build URI representing access to descendant documents of the given tree uri's document id
     *
     * @param treeUri    the subtree to leverage to gain access to the target document. The target directory must be a descendant of this subtree
     * @param documentId the target document, which the caller may not have direct access to
     */
    @SimpleFunction(description = "Builds document uri using tree uri and document id.")
    public String BuildDocumentUriUsingTree(String treeUri, String documentId) {
        return DocumentsContract.buildDocumentUriUsingTree(Uri.parse(treeUri), documentId).toString();
    }

    /**
     * Build URI representing the children of the target directory in a document provider
     *
     * @param treeUri          the subtree to leverage to gain access to the target document. The target directory must be a descendant of this subtree
     * @param parentDocumentId the document to return children for, which the caller may not have direct access to, and which must be a directory
     */
    @SimpleFunction(description = "Builds child documents uri using tree (parent document) uri and its parent document's id.")
    public String BuildChildDocumentsUriUsingTree(String treeUri, String parentDocumentId) {
        return DocumentsContract.buildChildDocumentsUriUsingTree(Uri.parse(treeUri), parentDocumentId).toString();
    }

    /**
     * Returns display name of given document
     *
     * @param uriString the document's uri
     */
    @SimpleFunction(description = "Returns display name of given document uri.")
    public String DisplayName(final String uriString) {
        try {
            return getStringValue(uriString, DocumentsContract.Document.COLUMN_DISPLAY_NAME);
        } catch (Exception e) {
            postError("DisplayName", e.getMessage());
        }
        return "";
    }

    /**
     * Returns size (in bytes) of given document uri
     *
     * @param uriString the document's uri
     */
    @SimpleFunction(description = "Returns size (in bytes) of given document uri.")
    public String Size(final String uriString) {
        try {
            return getStringValue(uriString, DocumentsContract.Document.COLUMN_SIZE);
        } catch (Exception e) {
            postError("Size", e.getMessage());
        }
        return "";
    }

    /**
     * Returns last modified time/epoch timestamp of given document uri
     *
     * @param uriString the document's uri
     */
    @SimpleFunction(description = "Returns last modified time/epoch timestamp of given document uri.")
    public String LastModifiedTime(final String uriString) {
        try {
            return getStringValue(uriString, DocumentsContract.Document.COLUMN_LAST_MODIFIED);
        } catch (Exception e) {
            postError("LastModifiedTime", e.getMessage());
        }
        return "";
    }

    /**
     * Returns mime type of given document uri
     *
     * @param uriString the document's uri
     */
    @SimpleFunction(description = "Returns mime type of given document uri.")
    public String MimeType(final String uriString) {
        try {
            return getStringValue(uriString, DocumentsContract.Document.COLUMN_MIME_TYPE);
        } catch (Exception e) {
            postError("MimeType", e.getMessage());
        }
        return "";
    }

    private String getStringValue(String documentUri, String projection) throws Exception {
        Cursor cursor = activity.getContentResolver().query(Uri.parse(documentUri),
                new String[]{projection},
                null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } finally {
            cursor.close();
        }
        return "";
    }

    /**
     * Returns whether document can be copied or not
     *
     * @param uriString the document's uri
     */
    @SimpleFunction(description = "Returns whether document can be copied or not.")
    public boolean IsCopySupported(final String uriString) {
        return isFlagTrue("IsCopySupported",
                Uri.parse(uriString),
                DocumentsContract.Document.FLAG_SUPPORTS_COPY);
    }

    /**
     * Returns whether document is movable or not
     *
     * @param uriString the document's uri
     */
    @SimpleFunction(description = "Returns whether document is movable or not.")
    public boolean IsMoveSupported(final String uriString) {
        return isFlagTrue("IsMoveSupported",
                Uri.parse(uriString),
                DocumentsContract.Document.FLAG_SUPPORTS_MOVE);
    }

    /**
     * Returns whether document is deletable or not
     *
     * @param uriString the document's uri
     */
    @SimpleFunction(description = "Returns whether document is deletable or not.")
    public boolean IsDeleteSupported(final String uriString) {
        return isFlagTrue("IsDeleteSupported",
                Uri.parse(uriString),
                DocumentsContract.Document.FLAG_SUPPORTS_DELETE);
    }

    /**
     * Returns whether document supports renaming
     *
     * @param uriString the document's uri
     */
    @SimpleFunction(description = "Returns whether document supports renaming.")
    public boolean IsRenameSupported(final String uriString) {
        return isFlagTrue("IsRenameSupported",
                Uri.parse(uriString),
                DocumentsContract.Document.FLAG_SUPPORTS_RENAME);
    }

    private boolean isFlagTrue(String method, Uri uri, int flag) {
        try {
            Cursor cursor = contentResolver.query(uri,
                    new String[]{DocumentsContract.Document.COLUMN_FLAGS},
                    null,
                    null,
                    null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    return cursor.getString(0).contains(String.valueOf(flag));
                }
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            postError(method, e.getMessage());
        }
        return false;
    }

    /**
     * Creates a new and empty document.If document already exists then an incremental value will be automatically suffixed
     *
     * @param parentDocumentUri the parent dir's uri
     * @param fileName          the name of the file
     * @param mimeType          the mime type of file to be created
     */
    @SimpleFunction(description = "Creates a new and empty document.If document already exists then an incremental value will be automatically suffixed.")
    public void CreateDocument(final String parentDocumentUri, final String fileName, final String mimeType) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    final String uri = DocumentsContract.createDocument(contentResolver,
                            Uri.parse(parentDocumentUri),
                            mimeType,
                            fileName).toString();
                    postCreateResult(uri);
                } catch (Exception e) {
                    postCreateResult(e.getMessage());
                }
            }
        });
    }

    private void postCreateResult(final String uriString) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DocumentCreated(uriString);
            }
        });
    }

    /**
     * Event invoked after `CreateDocument` method
     *
     * @param uriString document's uri if operation was successful else returns error message
     */
    @SimpleEvent(description = "Event invoked after creating document.Returns document's uri if operation was successful else returns error message.")
    public void DocumentCreated(String uriString) {
        EventDispatcher.dispatchEvent(this, "DocumentCreated", uriString);
    }

    /**
     * Writes text to given document
     *
     * @param documentUri the document's uri
     * @param text        text to write
     */
    @SimpleFunction(description = "Writes text to given document")
    public void WriteText(final String documentUri, final String text) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                if (!MimeType(documentUri).equals(DocumentDirMimeType())) {
                    String res;
                    try {
                        OutputStream fileOutputStream = contentResolver.openOutputStream(Uri.parse(documentUri),"wt");
                        res = writeToOutputStream(fileOutputStream, text);
                        res = res.isEmpty() ? documentUri : res;
                    } catch (Exception e) {
                        res = e.getMessage();
                    }
                    postWriteResult(res);
                } else {
                    postError("WriteText", "Can't write text to dir");
                }
            }
        });
    }

    private String writeToOutputStream(OutputStream fileOutputStream, String content) {
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(fileOutputStream);
            writer.write(content);
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        } finally {
            try {
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.flush();
                    fileOutputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    /**
     * Writes bytes to given document
     *
     * @param documentUri the document's uri
     * @param bytes       bytes to write
     */
    @SimpleFunction(description = "Writes bytes to given document")
    public void WriteBytes(final String documentUri, final Object bytes) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                if (!MimeType(documentUri).equals(DocumentDirMimeType())) {
                    try {
                        OutputStream outputStream = contentResolver.openOutputStream(Uri.parse(documentUri),"wt");
                        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
                        arrayOutputStream.write((byte[]) bytes);
                        arrayOutputStream.writeTo(outputStream);
                        postWriteResult(documentUri);
                    } catch (Exception e) {
                        postWriteResult(e.getMessage());
                    }
                } else {
                    postError("WriteBytes", "Can't write bytes to dir");
                }
            }
        });
    }

    private void postWriteResult(final String response) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GotWriteResult(response);
            }
        });
    }

    /**
     * Event invoked after writing document (`WriteText` and `WriteBytes` both methods trigger this event)
     *
     * @param response document's uri if operation was successful else returns error message
     */
    @SimpleEvent(description = "Event invoked after writing to document.Returns document's uri if operation was successful else returns error message")
    public void GotWriteResult(String response) {
        EventDispatcher.dispatchEvent(this, "GotWriteResult", response);
    }

    /**
     * Tries to delete document and returns delete operation's result
     *
     * @param documentUri the document's uri
     */
    @SimpleFunction(description = "Tries to delete document and returns result.")
    public boolean DeleteDocument(String documentUri) {
        try {
            return DocumentsContract.deleteDocument(contentResolver,
                    Uri.parse(documentUri));
        } catch (Exception e) {
            postError("DeleteDocument", e.getMessage());
        }
        return false;
    }

    /**
     * Reads from given document as text
     *
     * @param documentUri the document's uri
     */
    @SimpleFunction(description = "Reads from given document as text")
    public void ReadText(final String documentUri) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                if (!MimeType(documentUri).equals(DocumentDirMimeType())) {
                    String res;
                    try {
                        res = readFromInputStream(contentResolver.openInputStream(Uri.parse(documentUri)));
                    } catch (FileNotFoundException e) {
                        res = e.getMessage();
                    }
                    postReadResult(res);
                } else {
                    postError("ReadText", "Can't read text from dir");
                }
            }
        });
    }

    private String readFromInputStream(InputStream fileInputStream) {
        InputStreamReader input = new InputStreamReader(fileInputStream);
        try {
            StringWriter output = new StringWriter();
            int BUFFER_LENGTH = 4096;
            char[] buffer = new char[BUFFER_LENGTH];
            int offset = 0;
            int length;
            while ((length = input.read(buffer, offset, BUFFER_LENGTH)) > 0) {
                output.write(buffer, 0, length);
            }
            return normalizeNewLines(output.toString());
        } catch (Exception e) {
            return e.getMessage();
        } finally {
            try {
                input.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Reads content of document as bytes
     *
     * @param documentUri the document's uri
     */
    @SimpleFunction(description = "Reads content of document as bytes")
    public void ReadBytes(final String documentUri) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                if (!MimeType(documentUri).equals(DocumentDirMimeType())) {
                    try {
                        InputStream inputStream = contentResolver.openInputStream(Uri.parse(documentUri));
                        byte[] byteArray = new byte[Integer.parseInt(Size(documentUri))];
                        inputStream.read(byteArray);
                        inputStream.close();
                        postReadResult(byteArray);
                    } catch (Exception e) {
                        postReadResult(e.getMessage());
                    }
                } else {
                    postError("ReadBytes", "Can't read bytes from dir");
                }
            }
        });
    }

    /**
     * Event invoked after reading from document
     *
     * @param result returns content/text if operation was successful else returns error message
     */
    @SimpleEvent(description = "Event invoked after reading from document.Returns content if operation was successful else returns error message.")
    public void GotReadResult(Object result) {
        EventDispatcher.dispatchEvent(this, "GotReadResult", result);
    }

    private void postReadResult(final Object r) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GotReadResult(r);
            }
        });
    }

    private String normalizeNewLines(String s) {
        return s.replaceAll("\r\n", "\n");
    }

    /**
     * Checks whether read is granted for given uri
     *
     * @param uri the uri to check against
     */
    @SimpleFunction(description = "Returns whether read is granted for given uri.")
    public boolean IsReadGranted(String uri) {
        for (UriPermission uri1 : contentResolver.getPersistedUriPermissions()) {
            String str = uri1.getUri().toString();
            if (uri.equalsIgnoreCase(str)) {
                return uri1.isReadPermission();
            }
        }
        return false;
    }

    /**
     * Checks whether write is granted for given uri
     *
     * @param uri the uri to check against
     */
    @SimpleFunction(description = "Returns whether write is granted for given uri.")
    public boolean IsWriteGranted(String uri) {
        for (UriPermission uri1 : contentResolver.getPersistedUriPermissions()) {
            String str = uri1.getUri().toString();
            if (uri.equalsIgnoreCase(str)) {
                return uri1.isWritePermission();
            }
        }
        return false;
    }

    /**
     * Relinquish a persisted URI permission granted previously
     *
     * @param uri   the document's uri
     * @param flags the flags which will be released
     */
    @SimpleFunction(description = "Relinquish a persisted URI permission grant.")
    public void ReleasePermission(String uri, int flags) {
        contentResolver.releasePersistableUriPermission(Uri.parse(uri), flags);
    }

    /**
     * Event invoked when user selects a document or tree from SAF file picker
     *
     * @param uri       the uri object of document picked
     * @param uriString the string representation of uri
     */
    @SimpleEvent(description = "Event invoked when user selects a document or tree from SAF file picker.")
    public void GotUri(Object uri, String uriString) {
        EventDispatcher.dispatchEvent(this, "GotUri", uri, uriString);
    }

    /**
     * Tries to list documents from given document dir
     *
     * @param dirUri        the dir's uri to list documents from
     * @param dirDocumentId the document id of provided dir
     */
    @SimpleFunction(description = "Tries to list files from given document dir.")
    public void ListDocuments(final String dirUri, final String dirDocumentId) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                final List<String> list = listDocuments(Uri.parse(dirUri), dirDocumentId);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        GotDocumentsList(list);
                    }
                });
            }
        });
    }

    // taken from https://stackoverflow.com/questions/41096332/issues-traversing-through-directory-hierarchy-with-android-storage-access-framew
    private List<String> listDocuments(Uri treeUri, String documentId) {
        List<String> uriList = new ArrayList<>();
        Uri uriFolder = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId);
        try {
            Cursor cursor = contentResolver.query(uriFolder,
                    new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID},
                    null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    Uri uriFile = DocumentsContract.buildDocumentUriUsingTree(treeUri, cursor.getString(0));
                    uriList.add(uriFile.toString());
                    while (cursor.moveToNext()) {
                        uriFile = DocumentsContract.buildDocumentUriUsingTree(treeUri, cursor.getString(0));
                        uriList.add(uriFile.toString());
                    }
                }
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            postError("ListFiles", e.getMessage());
        }
        return uriList;
    }

    /**
     * Event invoked after getting files list
     *
     * @param documentsList the list of documents
     */
    @SimpleEvent(description = "Event invoked after getting files list.")
    public void GotDocumentsList(List<String> documentsList) {
        EventDispatcher.dispatchEvent(this, "GotDocumentsList", documentsList);
    }

    /**
     * Tries to copy document from source uri to target dir
     *
     * @param sourceUri       the source document's uri
     * @param targetParentUri the target dir's uri
     */
    @SimpleFunction(description = "Tries to copy document from source uri to target dir.")
    public void CopyDocument(final String sourceUri, final String targetParentUri) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                boolean successful = true;
                String response = "";
                try {
                    response = DocumentsContract.copyDocument(contentResolver,
                            Uri.parse(sourceUri),
                            Uri.parse(targetParentUri)).toString();
                } catch (Exception e) {
                    successful = false;
                    response = e.getMessage();
                }
                postCopyResult(successful, response);
            }
        });
    }

    private void postCopyResult(final boolean successful, final String response) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GotCopyResult(successful, response);
            }
        });
    }

    /**
     * Event invoked after getting copy operation's result
     *
     * @param successful returns whether operation was successful or not
     * @param response   returns newly created document's uri if operation was successful else returns error message
     */
    @SimpleEvent(description = "Event invoked after getting copy document result.Response will be newly created document's uri if operation was successful else returns error message.")
    public void GotCopyResult(boolean successful, String response) {
        EventDispatcher.dispatchEvent(this, "GotCopyResult", successful, response);
    }

    /**
     * Tries to move document from source uri to target dir
     *
     * @param sourceUri       the source document's uri
     * @param sourceParentUri the uri of source document's parent document
     * @param targetParentUri the target dir's uri
     */
    @SimpleFunction(description = "Tries to move document from source uri to target dir.")
    public void MoveDocument(final String sourceUri, final String sourceParentUri, final String targetParentUri) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                boolean successful = true;
                String response = "";
                try {
                    response = DocumentsContract.moveDocument(contentResolver,
                            Uri.parse(sourceUri),
                            Uri.parse(sourceParentUri),
                            Uri.parse(targetParentUri)).toString();
                } catch (Exception e) {
                    successful = false;
                    response = e.getMessage();
                }
                postMoveResult(successful, response);
            }
        });
    }

    private void postMoveResult(final boolean successful, final String response) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GotMoveResult(successful, response);
            }
        });
    }

    /**
     * Event invoked after getting move operation's result
     *
     * @param successful returns whether operation was successful or not
     * @param response   returns newly created document's uri if operation was successful else returns error message
     */
    @SimpleEvent(description = "Event invoked after getting move document result.Response will be newly created document's uri if operation was successful else returns error message.")
    public void GotMoveResult(boolean successful, String response) {
        EventDispatcher.dispatchEvent(this, "GotMoveResult", successful, response);
    }

    /**
     * Tries to rename a document and returns updated uri
     *
     * @param documentUri the document's uri
     * @param displayName the new display name of document
     */
    @SimpleFunction(description = "Tries to rename a document and returns updated uri.")
    public String RenameDocument(final String documentUri, final String displayName) {
        try {
            return DocumentsContract.renameDocument(contentResolver,
                    Uri.parse(documentUri),
                    displayName).toString();
        } catch (FileNotFoundException e) {
            postError("RenameDocument", e.getMessage());
            return "";
        }
    }
}
