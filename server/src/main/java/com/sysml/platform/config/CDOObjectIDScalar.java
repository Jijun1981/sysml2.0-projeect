package com.sysml.platform.config;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/** CDOObjectID 标量类型配置 用于处理 CDO Object ID 的序列化和反序列化 */
@Configuration
public class CDOObjectIDScalar {

  @Bean
  public RuntimeWiringConfigurer cdoObjectIDWiringConfigurer() {
    return wiringBuilder ->
        wiringBuilder.scalar(
            GraphQLScalarType.newScalar()
                .name("CDOObjectID")
                .description("CDO Object ID scalar type")
                .coercing(
                    new Coercing<String, String>() {
                      @Override
                      public String serialize(Object dataFetcherResult)
                          throws CoercingSerializeException {
                        if (dataFetcherResult instanceof String) {
                          return (String) dataFetcherResult;
                        }
                        throw new CoercingSerializeException(
                            "Unable to serialize " + dataFetcherResult + " as CDOObjectID");
                      }

                      @Override
                      public String parseValue(Object input) throws CoercingParseValueException {
                        if (input instanceof String) {
                          return (String) input;
                        }
                        throw new CoercingParseValueException(
                            "Unable to parse value " + input + " as CDOObjectID");
                      }

                      @Override
                      public String parseLiteral(Object input)
                          throws CoercingParseLiteralException {
                        if (input instanceof StringValue) {
                          return ((StringValue) input).getValue();
                        }
                        throw new CoercingParseLiteralException(
                            "Unable to parse literal " + input + " as CDOObjectID");
                      }
                    })
                .build());
  }
}
