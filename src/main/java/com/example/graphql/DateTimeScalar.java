package com.example.graphql;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DateTimeScalar {
    public static final GraphQLScalarType DATE_TIME = GraphQLScalarType.newScalar()
        .name("DateTime")
        .description("A custom scalar for java.time.LocalDateTime")
        .coercing(new Coercing<LocalDateTime, String>() {
            @Override
            public String serialize(Object dataFetcherResult, GraphQLContext graphQLContext, Locale locale) {
                return ((LocalDateTime) dataFetcherResult).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }

            @Override
            public LocalDateTime parseValue(Object input, GraphQLContext graphQLContext, Locale locale) {
                return LocalDateTime.parse(input.toString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }

            @Override
            public LocalDateTime parseLiteral(Value<?> input, CoercedVariables variables, GraphQLContext graphQLContext, Locale locale) {
                if (input instanceof StringValue) {
                    return LocalDateTime.parse(((StringValue) input).getValue(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
                return null;
            }
        })
        .build();
}