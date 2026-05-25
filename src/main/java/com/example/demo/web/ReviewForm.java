package com.example.demo.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReviewForm {
    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer cleanlinessRating;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer serviceRating;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer locationRating;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer valueRating;

    @NotBlank
    @Size(min = 50, max = 2000)
    private String content;

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Integer getCleanlinessRating() {
        return cleanlinessRating;
    }

    public void setCleanlinessRating(Integer cleanlinessRating) {
        this.cleanlinessRating = cleanlinessRating;
    }

    public Integer getServiceRating() {
        return serviceRating;
    }

    public void setServiceRating(Integer serviceRating) {
        this.serviceRating = serviceRating;
    }

    public Integer getLocationRating() {
        return locationRating;
    }

    public void setLocationRating(Integer locationRating) {
        this.locationRating = locationRating;
    }

    public Integer getValueRating() {
        return valueRating;
    }

    public void setValueRating(Integer valueRating) {
        this.valueRating = valueRating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
