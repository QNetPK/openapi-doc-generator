package io.github.swagger2markup.model;

import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;

public class BodyParameter extends Parameter {

  private final RequestBody requestBody;

  public BodyParameter(RequestBody requestBody) {
    this.requestBody = requestBody;
    super.setIn("body");
    super.setSchema(requestBody.getContent().values().iterator().next().getSchema());
    super.setRequired(requestBody.getRequired());
    super.setName("Request Body");
    super.set$ref(requestBody.get$ref());
    super.setDescription(requestBody.getDescription());
    super.setExtensions(requestBody.getExtensions());
  }

  public RequestBody getRequestBody() {
    return requestBody;
  }

}
