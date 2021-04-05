/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2018 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.zap.extension.airtableview;

import org.parosproxy.paros.extension.ExtensionAdaptor;
import org.parosproxy.paros.extension.ExtensionHook;
import org.parosproxy.paros.network.HttpMessage;
import org.zaproxy.zap.extension.httppanel.Message;
import org.zaproxy.zap.extension.httppanel.component.split.request.RequestSplitComponent;
import org.zaproxy.zap.extension.httppanel.component.split.response.ResponseSplitComponent;
import org.zaproxy.zap.extension.httppanel.view.HttpPanelDefaultViewSelector;
import org.zaproxy.zap.extension.httppanel.view.HttpPanelView;
import org.zaproxy.zap.extension.httppanel.view.impl.models.http.request.RequestBodyStringHttpPanelViewModel;
import org.zaproxy.zap.extension.httppanel.view.impl.models.http.response.ResponseBodyStringHttpPanelViewModel;
import org.zaproxy.zap.view.HttpPanelManager;
import org.zaproxy.zap.view.HttpPanelManager.HttpPanelDefaultViewSelectorFactory;
import org.zaproxy.zap.view.HttpPanelManager.HttpPanelViewFactory;

public class ExtensionHttpPanelAirtableView extends ExtensionAdaptor {

    public static final String NAME = "ExtensionHttpPanelAirtableView";

    public ExtensionHttpPanelAirtableView() {
        super(NAME);
    }

    @Override
    public void hook(ExtensionHook extensionHook) {
        super.hook(extensionHook);

        if (getView() != null) {
            HttpPanelManager panelManager = HttpPanelManager.getInstance();
            // add the view factories for http requests and responses
            panelManager.addRequestViewFactory(
                    RequestSplitComponent.NAME, new RequestAirtableViewFactory());
            panelManager.addResponseViewFactory(
                    ResponseSplitComponent.NAME, new ResponseAirtableViewFactory());
            // add default view selectors
            panelManager.addRequestDefaultViewSelectorFactory(
                    RequestSplitComponent.NAME,
                    new AirtableDefaultViewSelectorFactory(
                            RequestSplitComponent.NAME, RequestSplitComponent.ViewComponent.BODY));
            panelManager.addResponseDefaultViewSelectorFactory(
                    ResponseSplitComponent.NAME,
                    new AirtableDefaultViewSelectorFactory(
                            ResponseSplitComponent.NAME,
                            ResponseSplitComponent.ViewComponent.BODY));
        }
    }

    @Override
    public boolean canUnload() {
        return true;
    }

    @Override
    public void unload() {
        if (getView() != null) {
            HttpPanelManager panelManager = HttpPanelManager.getInstance();
            // remove views and their factories
            panelManager.removeRequestViewFactory(
                    RequestSplitComponent.NAME, RequestAirtableViewFactory.NAME);
            panelManager.removeRequestViews(
                    RequestSplitComponent.NAME,
                    HttpPanelAirtableView.NAME,
                    RequestSplitComponent.ViewComponent.BODY);
            panelManager.removeResponseViewFactory(
                    ResponseSplitComponent.NAME, ResponseAirtableViewFactory.NAME);
            panelManager.removeResponseViews(
                    ResponseSplitComponent.NAME,
                    HttpPanelAirtableView.NAME,
                    ResponseSplitComponent.ViewComponent.BODY);
            // remove default view selectors and their factories
            panelManager.removeRequestDefaultViewSelectorFactory(
                    RequestSplitComponent.NAME, AirtableDefaultViewSelectorFactory.NAME);
            panelManager.removeRequestDefaultViewSelectors(
                    RequestSplitComponent.NAME,
                    AirtableDefaultViewSelector.NAME,
                    RequestSplitComponent.ViewComponent.BODY);
            panelManager.removeResponseDefaultViewSelectorFactory(
                    ResponseSplitComponent.NAME, AirtableDefaultViewSelectorFactory.NAME);
            panelManager.removeResponseDefaultViewSelectors(
                    ResponseSplitComponent.NAME,
                    AirtableDefaultViewSelector.NAME,
                    ResponseSplitComponent.ViewComponent.BODY);
        }
        super.unload();
    }

    private static final class RequestAirtableViewFactory implements HttpPanelViewFactory {
        public static final String NAME = "RequestAirtableViewFactory";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public HttpPanelView getNewView() {
            return new HttpPanelAirtableView(new RequestBodyStringHttpPanelViewModel());
        }

        @Override
        public Object getOptions() {
            return RequestSplitComponent.ViewComponent.BODY;
        }
    }

    private static final class ResponseAirtableViewFactory implements HttpPanelViewFactory {
        public static final String NAME = "ResponseAirtableViewFactory";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public HttpPanelView getNewView() {
            return new HttpPanelAirtableView(new ResponseBodyStringHttpPanelViewModel());
        }

        @Override
        public Object getOptions() {
            return ResponseSplitComponent.ViewComponent.BODY;
        }
    }

    private static final class AirtableDefaultViewSelector implements HttpPanelDefaultViewSelector {
        private static final String NAME = "AirtableDefaultViewSelector";
        private Object options;

        AirtableDefaultViewSelector(Object options) {
            this.options = options;
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public boolean matchToDefaultView(Message message) {
            if (message == null) {
                // called with null when deleting the view, ie. when changing from split to combined
                // view
                return false;
            }
            if (message instanceof HttpMessage) {
                HttpMessage httpMessage = (HttpMessage) message;
                String bodyString;
                if (this.options == RequestSplitComponent.NAME) {
                    bodyString = httpMessage.getRequestBody().toString();
                } else if (this.options == ResponseSplitComponent.NAME) {
                    bodyString = httpMessage.getResponseBody().toString();
                } else {
                    return false;
                }
                if (bodyString == null || bodyString == "") {
                    return false;
                }
                if (bodyString.contains("stringifiedObjectParams")) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String getViewName() {
            return HttpPanelAirtableView.NAME;
        }

        @Override
        public int getOrder() {
            return 0;
        }
    }

    private static final class AirtableDefaultViewSelectorFactory
            implements HttpPanelDefaultViewSelectorFactory {
        public static final String NAME = "AirtableDefaultViewSelector";
        private Object selectorOptions;
        private Object factoryOptions;

        AirtableDefaultViewSelectorFactory(Object selectorOptions, Object factoryOptions) {
            this.factoryOptions = factoryOptions;
            this.selectorOptions = selectorOptions;
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public HttpPanelDefaultViewSelector getNewDefaultViewSelector() {
            return new AirtableDefaultViewSelector(this.selectorOptions);
        }

        @Override
        public Object getOptions() {
            return this.factoryOptions;
        }
    }
}
