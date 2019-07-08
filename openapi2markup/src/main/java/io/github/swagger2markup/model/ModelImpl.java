package io.github.swagger2markup.model;

import io.swagger.v3.oas.models.media.Schema;

/**
 * Marker for simple Impl.
 *
 */
public class ModelImpl<T> extends Schema<T> implements Model<T> {

  public ModelImpl(String type, String format) {
    super(type, format);
  }

}
