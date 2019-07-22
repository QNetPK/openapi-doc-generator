package io.github.swagger2markup.model;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.models.media.Schema;

/**
 * A schema defined under components.
 */
public interface Model<T> {

  Map<String, Schema> getProperties();

  String getTitle();

  String get$ref();

  String getDescription();

  Object getExample();

  String getFormat();

  String getType();

  List<String> getRequired();

}
