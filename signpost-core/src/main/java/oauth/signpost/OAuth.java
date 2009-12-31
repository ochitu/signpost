/* Copyright (c) 2008, 2009 Netflix, Matthias Kaeppler
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
package oauth.signpost;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gdata.util.common.base.PercentEscaper;

public class OAuth {

    public static final String VERSION_1_0 = "1.0";

    public static final String ENCODING = "UTF-8";

    public static final String FORM_ENCODED = "application/x-www-form-urlencoded";

    public static final String HTTP_AUTHORIZATION_HEADER = "Authorization";

    public static final String OAUTH_CONSUMER_KEY = "oauth_consumer_key";

    public static final String OAUTH_TOKEN = "oauth_token";

    public static final String OAUTH_TOKEN_SECRET = "oauth_token_secret";

    public static final String OAUTH_SIGNATURE_METHOD = "oauth_signature_method";

    public static final String OAUTH_SIGNATURE = "oauth_signature";

    public static final String OAUTH_TIMESTAMP = "oauth_timestamp";

    public static final String OAUTH_NONCE = "oauth_nonce";

    public static final String OAUTH_VERSION = "oauth_version";

    public static final String OAUTH_CALLBACK = "oauth_callback";

    public static final String OAUTH_CALLBACK_CONFIRMED = "oauth_callback_confirmed";

    public static final String OAUTH_VERIFIER = "oauth_verifier";

    public static final String OUT_OF_BAND = "oob";

    private static final PercentEscaper percentEncoder = new PercentEscaper(
            "-._~", false);

    public static String percentEncode(String s) {
        if (s == null) {
            return "";
        }
        return percentEncoder.escape(s);
    }

    public static String percentDecode(String s) {
        try {
            if (s == null) {
                return "";
            }
            return URLDecoder.decode(s, ENCODING);
            // This implements http://oauth.pbwiki.com/FlexibleDecoding
        } catch (java.io.UnsupportedEncodingException wow) {
            throw new RuntimeException(wow.getMessage(), wow);
        }
    }

    /**
     * Construct a x-www-form-urlencoded document containing the given sequence
     * of name/value pairs. Use OAuth percent encoding (not exactly the encoding
     * mandated by x-www-form-urlencoded).
     */
    public static <T extends Map.Entry<String, String>> void formEncode(Collection<T> parameters,
            OutputStream into) throws IOException {
        if (parameters != null) {
            boolean first = true;
            for (Map.Entry<String, String> entry : parameters) {
                if (first) {
                    first = false;
                } else {
                    into.write('&');
                }
                into.write(percentEncode(toString(entry.getKey())).getBytes());
                into.write('=');
                into.write(percentEncode(toString(entry.getValue())).getBytes());
            }
        }
    }

    /**
     * Construct a x-www-form-urlencoded document containing the given sequence
     * of name/value pairs. Use OAuth percent encoding (not exactly the encoding
     * mandated by x-www-form-urlencoded).
     */
    public static <T extends Map.Entry<String, String>> String formEncode(Collection<T> parameters)
            throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        formEncode(parameters, b);
        return new String(b.toByteArray());
    }

    /** Parse a form-urlencoded document. */
    public static Map<String, String> decodeForm(String form) {
        HashMap<String, String> params = new HashMap<String, String>();
        if (isEmpty(form)) {
            return params;
        }
        for (String nvp : form.split("\\&")) {
            int equals = nvp.indexOf('=');
            String name;
            String value;
            if (equals < 0) {
                name = percentDecode(nvp);
                value = null;
            } else {
                name = percentDecode(nvp.substring(0, equals));
                value = percentDecode(nvp.substring(equals + 1));
            }

            params.put(name, value);
        }
        return params;
    }

    public static Map<String, String> decodeForm(InputStream content)
            throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                content));
        StringBuilder sb = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            sb.append(line);
            line = reader.readLine();
        }

        return decodeForm(sb.toString());
    }

    /**
     * Construct a Map containing a copy of the given parameters. If several
     * parameters have the same name, the Map will contain the first value,
     * only.
     */
    public static <T extends Map.Entry<String, String>> Map<String, String> toMap(Collection<T> from) {
        HashMap<String, String> map = new HashMap<String, String>();
        if (from != null) {
            for (Map.Entry<String, String> entry : from) {
                String key = entry.getKey();
                if (!map.containsKey(key)) {
                    map.put(key, entry.getValue());
                }
            }
        }
        return map;
    }

    private static final String toString(Object from) {
        return (from == null) ? null : from.toString();
    }

    private static boolean isEmpty(String str) {
        return (str == null) || (str.length() == 0);
    }

    public static String addQueryParameters(String url, String... kvPairs) {
        String queryDelim = url.contains("?") ? "&" : "?";
        StringBuilder sb = new StringBuilder(url + queryDelim);
        for (int i = 0; i < kvPairs.length; i += 2) {
            if (i > 0) {
                sb.append("&");
            }
            sb.append(OAuth.percentEncode(kvPairs[i]) + "="
                    + OAuth.percentEncode(kvPairs[i + 1]));
        }
        return sb.toString();
    }

    public static Map<String, String> oauthHeaderToParamsMap(String oauthHeader) {
        if (oauthHeader == null || !oauthHeader.startsWith("OAuth ")) {
            return Collections.emptyMap();
        }
        oauthHeader = oauthHeader.substring("OAuth ".length());
        String[] elements = oauthHeader.split(",");
        HashMap<String, String> params = new HashMap<String, String>();
        for (String keyValuePair : elements) {
            String[] keyValue = keyValuePair.split("=");
            params.put(keyValue[0].trim(), keyValue[1].trim());
        }
        return params;
    }

}
