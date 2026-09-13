package com.travel.meomulkyung.itinerary.controller;

import com.travel.meomulkyung.itinerary.service.ItineraryException;
import com.travel.meomulkyung.itinerary.external.TourApiProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ItineraryExceptionHandler {
 private static final Logger log = LoggerFactory.getLogger(ItineraryExceptionHandler.class);
 record Error(int status, String code, String message) {}

 @ExceptionHandler(ItineraryException.class)
 ResponseEntity<Error> itinerary(ItineraryException exception) {
  return ResponseEntity.status(exception.status).body(new Error(exception.status.value(), exception.code, exception.getMessage()));
 }

 @ExceptionHandler(TourApiProviderException.class)
 ResponseEntity<Error> tourApiUnavailable(TourApiProviderException exception) {
  log.warn("tour_api_unavailable type={} causeClass={} httpStatus={}", exception.getFailureType(),
          exception.getCauseClassName(), exception.getHttpStatusCode());
  return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(new Error(HttpStatus.SERVICE_UNAVAILABLE.value(), "TOUR_API_UNAVAILABLE", "외부 관광 API를 현재 사용할 수 없습니다."));
 }

 @ExceptionHandler(MethodArgumentNotValidException.class)
 ResponseEntity<Error> invalidRequest(MethodArgumentNotValidException exception) {
  String field = exception.getBindingResult().getFieldError() == null ? null : exception.getBindingResult().getFieldError().getField();
  String code = "nights".equals(field) ? "INVALID_NIGHTS" : "preferenceTags".equals(field) ? "INVALID_PREFERENCE_TAGS" : "companionType".equals(field) ? "INVALID_COMPANION_TYPE" : "INVALID_COMPLETION_INPUT";
  return ResponseEntity.badRequest().body(new Error(400, code, "요청 값이 올바르지 않습니다."));
 }

 @ExceptionHandler(HttpMessageNotReadableException.class)
 ResponseEntity<Error> unreadable(HttpMessageNotReadableException exception) {
  String code = exception.getMessage() != null && exception.getMessage().contains("CompanionType") ? "INVALID_COMPANION_TYPE" : "INVALID_COMPLETION_INPUT";
  return ResponseEntity.badRequest().body(new Error(400, code, "요청 값이 올바르지 않습니다."));
 }
}
