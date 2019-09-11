/*
 * Copyright 2019 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.ksql.schema.ksql.types;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import com.google.common.testing.EqualsTester;
import io.confluent.ksql.schema.ksql.DataException;
import io.confluent.ksql.schema.ksql.Field;
import io.confluent.ksql.schema.ksql.FormatOptions;
import io.confluent.ksql.schema.ksql.SqlBaseType;
import io.confluent.ksql.types.KsqlStruct;
import io.confluent.ksql.util.KsqlException;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SqlStructTest {

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  @Test
  public void shouldImplementHashCodeAndEqualsProperly() {
    new EqualsTester()
        .addEqualityGroup(
            SqlStruct.builder().field("f0", SqlTypes.INTEGER).build(),
            SqlStruct.builder().field("f0", SqlTypes.INTEGER).build()
        )
        .addEqualityGroup(
            SqlStruct.builder().field("f1", SqlTypes.INTEGER).build()
        )
        .addEqualityGroup(
            SqlStruct.builder().field("f0", SqlTypes.BIGINT).build()
        )
        .testEquals();
  }

  @Test
  public void shouldReturnSqlType() {
    assertThat(SqlStruct.builder().field("f0", SqlTypes.BIGINT).build().baseType(),
        is(SqlBaseType.STRUCT));
  }

  @Test
  public void shouldReturnFields() {
    // When:
    final SqlStruct struct = SqlStruct.builder()
        .field("f0", SqlTypes.BIGINT)
        .field("f1", SqlTypes.DOUBLE)
        .build();

    // Then:
    assertThat(struct.fields(), contains(
        Field.of("f0", SqlTypes.BIGINT),
        Field.of("f1", SqlTypes.DOUBLE)
    ));
  }

  @Test
  public void shouldNotThrowIfNoFields() {
    SqlStruct.builder().build();
  }

  @Test
  public void shouldThrowOnDuplicateFieldName() {
    // Then:
    expectedException.expect(KsqlException.class);
    expectedException.expectMessage(
        "Duplicate field names found in STRUCT: '`F0` BOOLEAN' and '`F0` INTEGER'");

    // When:
    SqlStruct.builder()
        .field("F0", SqlPrimitiveType.of(SqlBaseType.BOOLEAN))
        .field("F0", SqlPrimitiveType.of(SqlBaseType.INTEGER));
  }

  @Test
  public void shouldNotThrowIfTwoFieldsHaveSameNameButDifferentCase() {
    // When:
    SqlStruct.builder()
        .field("f0", SqlPrimitiveType.of(SqlBaseType.BOOLEAN))
        .field("F0", SqlPrimitiveType.of(SqlBaseType.BOOLEAN))
        .build();

    // Then: did not throw.
  }

  @Test
  public void shouldImplementToString() {
    // Given:
    final SqlStruct struct = SqlStruct.builder()
        .field("f0", SqlTypes.BIGINT)
        .field("F1", SqlTypes.array(SqlTypes.DOUBLE))
        .build();

    // When:
    final String sql = struct.toString();

    // Then:
    assertThat(sql, is(
        "STRUCT<"
            + "`f0` " + SqlTypes.BIGINT
            + ", `F1` " + SqlTypes.array(SqlTypes.DOUBLE)
            + ">"
    ));
  }

  @Test
  public void shouldImplementToStringForEmptyStruct() {
    // Given:
    final SqlStruct emptyStruct = SqlStruct.builder().build();

    // When:
    final String sql = emptyStruct.toString();

    // Then:
    assertThat(sql, is("STRUCT< >"));
  }

  @Test
  public void shouldImplementToStringWithReservedWordHandling() {
    // Given:
    final SqlStruct struct = SqlStruct.builder()
        .field("f0", SqlTypes.BIGINT)
        .field("F1", SqlTypes.array(SqlTypes.DOUBLE))
        .build();

    final FormatOptions formatOptions = FormatOptions.of(word -> word.equals("F1"));

    // When:
    final String sql = struct.toString(formatOptions);

    // Then:
    assertThat(sql, is(
        "STRUCT<"
            + "f0 " + SqlTypes.BIGINT
            + ", `F1` " + SqlTypes.array(SqlTypes.DOUBLE)
            + ">"
    ));
  }

  @Test
  public void shouldThrowIfValueNotStruct() {
    // Given:
    final SqlStruct schema = SqlTypes.struct()
        .field("f0", SqlTypes.BIGINT)
        .build();

    // Then:
    expectedException.expect(DataException.class);
    expectedException.expectMessage("Expected STRUCT, got BIGINT");

    // When:
    schema.validateValue(10L);
  }

  @Test
  public void shouldThrowIfValueHasSchema() {
    // Given:
    final SqlStruct schema = SqlTypes.struct()
        .field("f0", SqlTypes.BIGINT)
        .build();

    final SqlStruct mismatching = SqlTypes.struct()
        .field("f0", SqlTypes.DOUBLE)
        .build();

    final KsqlStruct value = KsqlStruct.builder(mismatching)
        .set("f0", Optional.of(10.0D))
        .build();

    // Then:
    expectedException.expect(DataException.class);
    expectedException.expectMessage("Expected STRUCT<`f0` BIGINT>, got STRUCT<`f0` DOUBLE>");

    // When:
    schema.validateValue(value);
  }

  @Test
  public void shouldNotThrowWhenValidatingNullValue() {
    // Given:
    final SqlStruct schema = SqlTypes.struct()
        .field("f0", SqlTypes.BIGINT)
        .build();

    // When:
    schema.validateValue(null);
  }

  @Test
  public void shouldValidateValue() {
    // Given:
    final SqlStruct schema = SqlTypes.struct()
        .field("f0", SqlTypes.BIGINT)
        .build();

    final KsqlStruct value = KsqlStruct.builder(schema)
        .set("f0", Optional.of(10L))
        .build();

    // When:
    schema.validateValue(value);
  }
}