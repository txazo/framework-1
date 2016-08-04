/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package leap.web.api.mvc;

import leap.lang.http.HTTP;
import leap.web.Response;

public class DefaultApiErrorHandler implements ApiErrorHandler {

    @Override
    public void unauthorized(Response response) {
        unauthorized(response, "Unauthorized");
    }

    @Override
    public void unauthorized(Response response, String message) {
        responseError(response, HTTP.SC_UNAUTHORIZED, "Unauthorized", message);
    }

    @Override
    public void forbidden(Response response) {
        forbidden(response, "Forbidden");
    }

    @Override
    public void forbidden(Response response, String message) {
        responseError(response, HTTP.SC_FORBIDDEN, "Forbidden", message);
    }

    @Override
    public void notFound(Response response) {
        notFound(response, "Not found");
    }

    @Override
    public void notFound(Response response, String message) {
        responseError(response, HTTP.SC_NOT_FOUND, "NotFound", message);
    }

    @Override
    public void badRequest(Response response) {
        badRequest(response, "Bad request");
    }

    @Override
    public void badRequest(Response response, String message) {
        responseError(response, HTTP.SC_BAD_REQUEST, "BadRequest", message);
    }

    @Override
    public void internalServerError(Response response, String message) {
        responseError(response, HTTP.SC_INTERNAL_SERVER_ERROR, "InternalServerError", message);
    }

    @Override
    public void internalServerError(Response response, Throwable cause) {
        responseError(response, HTTP.SC_INTERNAL_SERVER_ERROR, "InternalServerError", cause.getMessage());
    }

    @Override
    public void responseError(Response response, int status, String message) {
        responseError(response, status, new ApiError(message));
    }

    protected void responseError(Response response, int status, String code, String message) {
        responseError(response, status, new ApiError(code, message));
    }

    @Override
    public void responseError(Response response, int status, ApiError error) {
        response.setStatus(status);
        error.response(response);
    }

}
