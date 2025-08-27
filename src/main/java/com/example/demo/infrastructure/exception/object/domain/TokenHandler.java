package com.example.demo.infrastructure.exception.object.domain;

public class TokenHandler extends RuntimeException {
  public TokenHandler(String message) {
    super(message);
  }
}
