package com.psddev.cms.tool.page;

import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

@RoutingFilter.Path(application = "cms", value = "filePreview")
public class FilePreview extends PageServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolPageContext.class);
    private static final String PARAM_DISPLAY_PROGRESS = "displayProgress";

    public static void reallyDoService(ToolPageContext page) throws IOException, ServletException {

        if (page.paramOrDefault(boolean.class, PARAM_DISPLAY_PROGRESS, false)) {
            writeFileUploadPreview(page);
            return;
        }

        HttpServletRequest request = page.getRequest();
        State state = State.getInstance(request.getAttribute("object"));
        ObjectField field = (ObjectField) request.getAttribute("field");
        String inputName = ObjectUtils.firstNonBlank(page.param(String.class, "inputName"), (String) request.getAttribute("inputName"));
        String storageName = inputName + ".storage";
        String pathName = inputName + ".path";
        String contentTypeName = inputName + ".contentType";

        String fieldName = field != null ? field.getInternalName() : page.paramOrDefault(String.class, "fieldName", "");
        StorageItem fieldValue = null;

        //TODO: move somewhere reusable?
        //handle inline upload display
        if (page.paramOrDefault(Boolean.class, "isNewUpload", false)) {
            String storageItemPath = page.param(String.class, pathName);
            if (!StringUtils.isBlank(storageItemPath)) {
                StorageItem newItem = StorageItem.Static.createIn(page.param(storageName));
                newItem.setPath(page.param(pathName));
                //newItem.setContentType(page.param(contentTypeName));
                fieldValue = newItem;
            }

            state = State.getInstance(ObjectType.getInstance(page.param(UUID.class, "typeId")));
        }

        if (fieldValue == null) {
            fieldValue = (StorageItem) state.getValue(fieldName);
        }

        if (fieldValue == null) {
            return;
        }

        String contentType = fieldValue.getContentType();

        page.writeStart("div",
                "class", FileSelector.FILE_SELECTOR_EXISTING_CLASS + " " + FileSelector.FILE_SELECTOR_ITEM_CLASS + " filePreview");
            page.writeTag("input",
                    "name", page.h(storageName),
                    "type", "hidden",
                    "value", page.h(fieldValue.getStorage()));
            page.writeTag("input",
                    "name", page.h(pathName),
                    "type", "hidden",
                    "value", page.h(fieldValue.getPath()));
            page.writeTag("input",
                    "name", page.h(contentTypeName),
                    "type", "hidden",
                    "value", page.h(fieldValue.getContentType()));

            if (field != null && field.as(ToolUi.class).getStoragePreviewProcessorPath() != null) {
                ToolUi ui = field.as(ToolUi.class);
                String processorPath = ui.getStoragePreviewProcessorPath();
                if (processorPath != null) {
                    page.include(RoutingFilter.Static.getApplicationPath(ui.getStoragePreviewProcessorApplication()) +
                            StringUtils.ensureStart(processorPath, "/"));
                }
            } else {

                FileFieldWriter fileFieldWriter = FileFieldWriter.Static.getFileFieldWriter(fieldValue);
                if (fileFieldWriter != null) {
                    fileFieldWriter.writePreview(page);
                } else {
                    page.writeStart("a",
                            "href", page.h(fieldValue.getPublicUrl()),
                            "target", "_blank");
                        page.writeHtml(page.h(contentType) + ":" + page.h(fieldValue.getPath()));
                    page.writeEnd();
                }
            }
        page.writeEnd();
    }

    public static void writeFileUploadPreview(ToolPageContext page) throws  IOException, ServletException {

        page.writeStart("div", "class", "upload-preview loading");
            page.writeTag("img");
                page.writeStart("div",
                        "class", "radial-progress",
                        "data-progress", "0");
                    page.writeStart("div", "class", "circle");
                        page.writeStart("div", "class", "mask full");
                            page.writeStart("div", "class", "fill");
                            page.writeEnd();
                        page.writeEnd();
                        page.writeStart("div", "class", "mask half");
                            page.writeStart("div", "class", "fill");
                            page.writeEnd();
                        page.writeStart("div", "class", "fill fix");
                        page.writeEnd();
                    page.writeEnd();
                page.writeEnd();
                page.writeStart("div", "class", "inset");
                    page.writeStart("div", "class", "percentage");
                        page.writeStart("span");
                        page.writeEnd();
                    page.writeEnd();
                page.writeEnd();
            page.writeEnd();
        page.writeEnd();
    }

    public static void setMetadata(ToolPageContext page, State state, StorageItem fieldValue) throws IOException, ServletException {

        FileFieldWriter fileFieldWriter = FileFieldWriter.Static.getFileFieldWriter(fieldValue);

        if (fileFieldWriter == null) {
            return;
        }

        fileFieldWriter.setMetadata(page, state, fieldValue);
    }

    //TODO: where should this go?
    public static String createStorageItemPath(String fileName) {
        String idString = UUID.randomUUID().toString().replace("-", "");
        StringBuilder pathBuilder = new StringBuilder();
        //String label = state.getLabel();
        String label = null;
        String extension = "";

        if (!ObjectUtils.isBlank(fileName)) {
            int lastDotAt = fileName.indexOf('.');

            if (lastDotAt > -1) {
                extension = fileName.substring(lastDotAt);
                fileName = fileName.substring(0, lastDotAt);

            }
        }

        if (ObjectUtils.isBlank(label) ||
                ObjectUtils.to(UUID.class, label) != null) {
            label = fileName;
        }

        if (ObjectUtils.isBlank(label)) {
            label = UUID.randomUUID().toString().replace("-", "");
        }

        pathBuilder.append(idString.substring(0, 2));
        pathBuilder.append('/');
        pathBuilder.append(idString.substring(2, 4));
        pathBuilder.append('/');
        pathBuilder.append(idString.substring(4));
        pathBuilder.append('/');
        pathBuilder.append(StringUtils.toNormalized(label));
        pathBuilder.append(extension);

        return pathBuilder.toString();
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        reallyDoService(page);
    }

    @Override
    protected String getPermissionId() {
        return null;
    }
}