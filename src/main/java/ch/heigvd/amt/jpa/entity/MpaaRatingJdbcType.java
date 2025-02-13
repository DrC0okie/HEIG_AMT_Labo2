package ch.heigvd.amt.jpa.entity;


import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.SqlTypes;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class MpaaRatingJdbcType implements UserType<Film.MpaaRating> {

    @Override
    public int getSqlType() {
        return SqlTypes.ENUM;
    }

    @Override
    public Class<Film.MpaaRating> returnedClass() {
        return Film.MpaaRating.class;
    }

    @Override
    public boolean equals(Film.MpaaRating x, Film.MpaaRating y) {
        return Objects.equals(x,y);
    }

    @Override
    public int hashCode(Film.MpaaRating x) {
        return Objects.hashCode(x);
    }

    @Override
    public Film.MpaaRating nullSafeGet(ResultSet rs, int position,
                                       SharedSessionContractImplementor session, Object owner) throws SQLException {
        String columnValue = rs.getString(position);
        if (rs.wasNull()) {
            columnValue = null;
        }
        var value = Film.MpaaRating.getFromValue(columnValue);
        return value.orElse(null);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Film.MpaaRating value, int index,
                            SharedSessionContractImplementor session) throws SQLException {
        if (value == null) {
            st.setNull(index, SqlTypes.OTHER);
        }
        else {
            String stringValue = value.value();
            st.setObject(index, stringValue, SqlTypes.OTHER);
        }
    }

    @Override
    public Film.MpaaRating deepCopy(Film.MpaaRating value) {
        return value == null ? null : value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Film.MpaaRating value) {
        return deepCopy(value);
    }

    @Override
    public Film.MpaaRating assemble(Serializable cached, Object owner) {
        return deepCopy((Film.MpaaRating) cached);
    }

}