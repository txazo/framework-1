/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package leap.web.api.restd.crud;

import leap.core.value.Record;
import leap.lang.Strings;
import leap.orm.dao.Dao;
import leap.web.action.ActionParams;
import leap.web.action.FuncActionBuilder;
import leap.web.api.Api;
import leap.web.api.config.ApiConfigurator;
import leap.web.api.meta.ApiMetadata;
import leap.web.api.meta.model.MApiModel;
import leap.web.api.mvc.ApiResponse;
import leap.web.api.orm.CreateOneResult;
import leap.web.api.orm.ModelCreateExecutor;
import leap.web.api.orm.ModelExecutorContext;
import leap.web.api.orm.SimpleModelExecutorContext;
import leap.web.api.restd.CrudOperation;
import leap.web.api.restd.CrudOperationBase;
import leap.web.api.restd.RestdContext;
import leap.web.api.restd.RestdModel;
import leap.web.route.RouteBuilder;

import java.util.Map;
import java.util.function.Function;

/**
 * Create a new record operation.
 */
public class CreateOperation extends CrudOperationBase implements CrudOperation {

    protected static final String NAME = "create";

    @Override
    public void createCrudOperation(ApiConfigurator c, RestdContext context, RestdModel model) {
        if(!context.getConfig().allowCreateModel(model.getName())) {
            return;
        }

        String verb = "POST";
        String path = fullModelPath(c, model);

        FuncActionBuilder action = new FuncActionBuilder();
        RouteBuilder      route  = rm.createRoute(verb, path);

        if(isOperationExists(context, route)) {
            return;
        }

        action.setName(Strings.lowerCamel(NAME, model.getName()));
        action.setFunction(createFunction(c, context, model));
        addModelArgumentForCreate(context, action, model);
        addModelResponse(action, model).setStatus(201);

        preConfigure(context, model, action);
        route.setAction(action.build());
        setCrudOperation(route, NAME);

        postConfigure(context, model, route);

        if(isOperationExists(context, route)) {
            return;
        }

        c.addDynamicRoute(rm.loadRoute(context.getRoutes(), route));
    }

    protected Function<ActionParams,Object> createFunction(ApiConfigurator c, RestdContext context, RestdModel model) {
        return new CreateFunction(context.getApi(), context.getDao(), model);
    }

    protected class CreateFunction extends CrudFunction {
        public CreateFunction(Api api, Dao dao, RestdModel model) {
            super(api, dao, model);
        }

        @Override
        public Object apply(ActionParams params) {
            MApiModel am = api.getMetadata().getModel(model.getName());

            Map<String,Object> record = params.get(0);

            ModelExecutorContext context = new SimpleModelExecutorContext(api, am, dao, model.getEntityMapping());
            ModelCreateExecutor executor = newCreateExecutor(context);

            CreateOneResult result = executor.createOne(record);

            Record r = dao.find(model.getEntityMapping(), result.id);
            r.put("$id", result.id);

            return ApiResponse.created(r);
        }

        protected ModelCreateExecutor newCreateExecutor(ModelExecutorContext context) {
            return mef.newCreateExecutor(context);
        }

        @Override
        public String toString() {
            return "Function:" + "Create " + model.getName() + "";
        }
    }

}
