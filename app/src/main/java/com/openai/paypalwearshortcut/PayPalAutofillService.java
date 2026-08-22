package com.openai.paypalwearshortcut;

import android.app.assist.AssistStructure;
import android.content.ComponentName;
import android.os.CancellationSignal;
import android.service.autofill.AutofillService;
import android.service.autofill.Dataset;
import android.service.autofill.FillCallback;
import android.service.autofill.FillContext;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveInfo;
import android.service.autofill.SaveRequest;
import android.text.InputType;
import android.util.Pair;
import android.view.ViewStructure;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PayPalAutofillService extends AutofillService {
    private static final String SAMSUNG_INTERNET_PACKAGE = "com.sec.android.app.sbrowser";

    @Override
    public void onFillRequest(FillRequest request, CancellationSignal cancellationSignal, FillCallback callback) {
        try {
            AssistStructure structure = latestStructure(request.getFillContexts());
            if (structure == null || !isAllowedPayPalContext(structure)) {
                callback.onSuccess(null);
                return;
            }

            FieldSet fields = inspect(structure);
            if (fields.passwordId == null && fields.usernameId == null) {
                callback.onSuccess(null);
                return;
            }

            FillResponse.Builder response = new FillResponse.Builder();

            if (CredentialStore.hasCredential(this)) {
                String account = CredentialStore.getAccount(this);
                String password = CredentialStore.getPassword(this);
                if (!account.isEmpty() && !password.isEmpty()) {
                    RemoteViews presentation = new RemoteViews(getPackageName(), android.R.layout.simple_list_item_1);
                    presentation.setTextViewText(android.R.id.text1, "PayPal · " + maskAccount(account));

                    Dataset.Builder dataset = new Dataset.Builder(presentation);
                    if (fields.usernameId != null) {
                        dataset.setValue(fields.usernameId, AutofillValue.forText(account));
                    }
                    if (fields.passwordId != null) {
                        dataset.setValue(fields.passwordId, AutofillValue.forText(password));
                    }
                    response.addDataset(dataset.build());
                }
            }

            AutofillId[] saveIds = fields.saveIds();
            if (saveIds.length > 0) {
                int types = 0;
                if (fields.usernameId != null) types |= SaveInfo.SAVE_DATA_TYPE_USERNAME;
                if (fields.passwordId != null) types |= SaveInfo.SAVE_DATA_TYPE_PASSWORD;
                response.setSaveInfo(new SaveInfo.Builder(types, saveIds)
                        .setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
                        .build());
            }

            callback.onSuccess(response.build());
        } catch (Exception e) {
            callback.onSuccess(null);
        }
    }

    @Override
    public void onSaveRequest(SaveRequest request, SaveCallback callback) {
        try {
            AssistStructure structure = latestStructure(request.getFillContexts());
            if (structure == null || !isAllowedPayPalContext(structure)) {
                callback.onFailure("Not a verified PayPal login page");
                return;
            }

            FieldSet fields = inspect(structure);
            String account = fields.usernameValue;
            String password = fields.passwordValue;

            if (account != null && !account.trim().isEmpty() && password != null && !password.isEmpty()) {
                CredentialStore.saveCredential(this, account.trim(), password);
                callback.onSuccess();
            } else {
                callback.onFailure("PayPal username or password was not available");
            }
        } catch (Exception e) {
            callback.onFailure("Could not save PayPal login");
        }
    }

    private AssistStructure latestStructure(List<FillContext> contexts) {
        if (contexts == null || contexts.isEmpty()) return null;
        return contexts.get(contexts.size() - 1).getStructure();
    }

    private boolean isAllowedPayPalContext(AssistStructure structure) {
        DomainScan scan = new DomainScan();
        for (int w = 0; w < structure.getWindowNodeCount(); w++) {
            scanDomains(structure.getWindowNodeAt(w).getRootViewNode(), scan);
        }
        if (scan.verifiedPayPalDomain) return true;

        ComponentName activity = structure.getActivityComponent();
        String pkg = activity == null ? "" : activity.getPackageName();
        return SAMSUNG_INTERNET_PACKAGE.equals(pkg) && CredentialStore.isRecentExpectedPayPalLaunch(this);
    }

    private void scanDomains(AssistStructure.ViewNode node, DomainScan scan) {
        if (node == null || scan.verifiedPayPalDomain) return;
        String domain = node.getWebDomain();
        if (isPayPalDomain(domain)) {
            scan.verifiedPayPalDomain = true;
            return;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            scanDomains(node.getChildAt(i), scan);
        }
    }

    private boolean isPayPalDomain(String domain) {
        if (domain == null) return false;
        String d = domain.toLowerCase(Locale.US);
        return d.equals("paypal.com") || d.endsWith(".paypal.com") ||
                d.equals("paypal.me") || d.endsWith(".paypal.me");
    }

    private FieldSet inspect(AssistStructure structure) {
        FieldSet fields = new FieldSet();
        for (int w = 0; w < structure.getWindowNodeCount(); w++) {
            inspectNode(structure.getWindowNodeAt(w).getRootViewNode(), fields);
        }
        return fields;
    }

    private void inspectNode(AssistStructure.ViewNode node, FieldSet fields) {
        if (node == null) return;

        AutofillId id = node.getAutofillId();
        if (id != null) {
            FieldKind kind = classify(node);
            AutofillValue value = node.getAutofillValue();
            String text = value != null && value.isText() && value.getTextValue() != null
                    ? value.getTextValue().toString() : null;

            if (kind == FieldKind.PASSWORD && fields.passwordId == null) {
                fields.passwordId = id;
                fields.passwordValue = text;
            } else if (kind == FieldKind.USERNAME && fields.usernameId == null) {
                fields.usernameId = id;
                fields.usernameValue = text;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            inspectNode(node.getChildAt(i), fields);
        }
    }

    private FieldKind classify(AssistStructure.ViewNode node) {
        String[] hints = node.getAutofillHints();
        if (hints != null) {
            for (String hint : hints) {
                String h = lower(hint);
                if (h.contains("password")) return FieldKind.PASSWORD;
                if (h.contains("username") || h.contains("email")) return FieldKind.USERNAME;
            }
        }

        ViewStructure.HtmlInfo html = node.getHtmlInfo();
        if (html != null && html.getAttributes() != null) {
            for (Pair<String, String> attr : html.getAttributes()) {
                String key = lower(attr.first);
                String value = lower(attr.second);
                if ((key.equals("type") && value.equals("password")) ||
                        (key.equals("autocomplete") && value.contains("password"))) {
                    return FieldKind.PASSWORD;
                }
                if ((key.equals("type") && value.equals("email")) ||
                        (key.equals("autocomplete") && (value.contains("username") || value.contains("email")))) {
                    return FieldKind.USERNAME;
                }
                if ((key.equals("name") || key.equals("id")) && value.contains("pass")) return FieldKind.PASSWORD;
                if ((key.equals("name") || key.equals("id")) &&
                        (value.contains("user") || value.contains("email") || value.contains("login"))) {
                    return FieldKind.USERNAME;
                }
            }
        }

        int variation = node.getInputType() & InputType.TYPE_MASK_VARIATION;
        if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) {
            return FieldKind.PASSWORD;
        }
        if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) {
            return FieldKind.USERNAME;
        }

        String idEntry = lower(node.getIdEntry());
        if (idEntry.contains("pass")) return FieldKind.PASSWORD;
        if (idEntry.contains("user") || idEntry.contains("email") || idEntry.contains("login")) return FieldKind.USERNAME;

        return FieldKind.NONE;
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US);
    }

    private String maskAccount(String account) {
        int at = account.indexOf('@');
        if (at > 1) return account.substring(0, 1) + "***" + account.substring(at);
        if (account.length() <= 3) return "***";
        return account.substring(0, 2) + "***" + account.substring(account.length() - 1);
    }

    private enum FieldKind { NONE, USERNAME, PASSWORD }

    private static final class DomainScan {
        boolean verifiedPayPalDomain;
    }

    private static final class FieldSet {
        AutofillId usernameId;
        AutofillId passwordId;
        String usernameValue;
        String passwordValue;

        AutofillId[] saveIds() {
            ArrayList<AutofillId> ids = new ArrayList<>();
            if (usernameId != null) ids.add(usernameId);
            if (passwordId != null) ids.add(passwordId);
            return ids.toArray(new AutofillId[0]);
        }
    }
}
