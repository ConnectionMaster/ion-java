// Copyright (c) 2007-2009 Amazon.com, Inc.  All rights reserved.

package com.amazon.ion.impl;

import com.amazon.ion.Decimal;
import com.amazon.ion.IonDecimal;
import com.amazon.ion.IonException;
import com.amazon.ion.IonNumber;
import com.amazon.ion.IonType;
import com.amazon.ion.NullValueException;
import com.amazon.ion.ValueVisitor;
import java.io.IOException;
import java.math.BigDecimal;


/**
 * Implements the Ion <code>decimal</code> type.
 */
public final class IonDecimalImpl
    extends IonValueImpl
    implements IonDecimal
{

    static final int NULL_DECIMAL_TYPEDESC =
        IonConstants.makeTypeDescriptor(IonConstants.tidDecimal,
                                        IonConstants.lnIsNullAtom);
    static final int ZERO_DECIMAL_TYPEDESC =
        IonConstants.makeTypeDescriptor(IonConstants.tidDecimal,
                                        IonConstants.lnNumericZero);

    public static boolean isNegativeZero(float value)
    {
    	if (value != 0) return false;
        // TODO perhaps use Float.compare() instead?
    	if ((Float.floatToRawIntBits(value) & 0x80000000) == 0) return false; // test the sign bit
    	return true;
    }

    public static boolean isNegativeZero(double value)
    {
    	if (value != 0) return false;
    	// TODO perhaps use Double.compare() instead?
    	if ((Double.doubleToLongBits(value) & 0x8000000000000000L) == 0) return false;
    	return true;
    }

    private BigDecimal _decimal_value;

    /**
     * Constructs a <code>null.decimal</code> element.
     */
    public IonDecimalImpl(IonSystemImpl system)
    {
        super(system, NULL_DECIMAL_TYPEDESC);
        _hasNativeValue = true; // Since this is null
    }

    public IonDecimalImpl(IonSystemImpl system, BigDecimal value)
    {
        super(system, NULL_DECIMAL_TYPEDESC);
        _decimal_value = value;
        _hasNativeValue = true;
        assert isDirty();
    }

    /**
     * Constructs a binary-backed element.
     */
    public IonDecimalImpl(IonSystemImpl system, int typeDesc)
    {
        super(system, typeDesc);
        assert pos_getType() == IonConstants.tidDecimal;
    }

    /**
     * makes a copy of this IonDecimal including a copy
     * of the BigDecimal value which is "naturally" immutable.
     * This calls IonValueImpl to copy the annotations and the
     * field name if appropriate.  The symbol table is not
     * copied as the value is fully materialized and the symbol
     * table is unnecessary.
     */
    @Override
    public IonDecimalImpl clone()
    {
        IonDecimalImpl clone = new IonDecimalImpl(_system);

        makeReady();
        clone.copyAnnotationsFrom(this);
        clone.setValue(this._decimal_value);

        return clone;
    }


    public IonType getType()
    {
        return IonType.DECIMAL;
    }


    public float floatValue()
        throws NullValueException
    {
        makeReady();
        if (_decimal_value == null) throw new NullValueException();
        float f = _decimal_value.floatValue();
        return f;
    }

    public double doubleValue()
        throws NullValueException
    {
        makeReady();
        if (_decimal_value == null) throw new NullValueException();
        double d = _decimal_value.doubleValue();
        return d;
    }

    @Deprecated
    public BigDecimal toBigDecimal()
        throws NullValueException
    {
        return bigDecimalValue();
    }

    public BigDecimal bigDecimalValue()
        throws NullValueException
    {
        makeReady();
        return Decimal.bigDecimalValue(_decimal_value); // Works for null.
    }

    public Decimal decimalValue()
        throws NullValueException
    {
        makeReady();
        return Decimal.valueOf(_decimal_value); // Works for null.
    }

    public void setValue(long value)
    {
        // base setValue will check for the lock
        setValue(Decimal.valueOf(value));
    }

    // TODO need to specify whether float->IonDecimal is via print+parse
    public void setValue(float value)
    {
        // base setValue will check for the lock
        setValue(Decimal.valueOf(value));
    }

    // TODO need to specify whether double->IonDecimal is via print+parse
    public void setValue(double value)
    {
        // base setValue will check for the lock
        setValue(Decimal.valueOf(value));
    }

    public void setValue(BigDecimal value)
    {
        checkForLock();
        _decimal_value = value;
        _hasNativeValue = true;
        setDirty();
    }

    @Deprecated
    public void setValueSpecial(IonNumber.Classification valueClassification)
    {
    	setValue(BigDecimal.ZERO, valueClassification);
    }

    @Deprecated
    public void setValue(BigDecimal value, IonNumber.Classification valueClassification)
    {
        checkForLock();
        switch (valueClassification)
        {
        case NORMAL:
                _decimal_value = value;
        	break;
        case NEGATIVE_ZERO:
        	if (value.signum() != 0) throw new IllegalArgumentException("to be a negative zero the value must be zero");
        	_decimal_value = Decimal.negativeZero(value.scale());
        	break;
        default:
        	throw new IllegalArgumentException("IonDecimal values may only be NORMAL or NEGATIVE_ZERO");
        }
        _hasNativeValue = true;
        setDirty();
    }

    /**
     * Gets the number classification of this value. IonDecimal
     * values may only be NORMAL or NEGATIVE_ZERO.
     */
    public IonNumber.Classification getClassification()
    {
        makeReady();
    	return (isNullValue() || ! Decimal.isNegativeZero(_decimal_value)
    	            ? Classification.NORMAL
    	            : Classification.NEGATIVE_ZERO);
    }

    @Override
    public synchronized boolean isNullValue()
    {
        if (!_hasNativeValue) return super.isNullValue();
        return (_decimal_value == null);
    }

    @Override
    protected int getNativeValueLength()
    {
        assert _hasNativeValue == true;
        return IonBinary.lenIonDecimal(_decimal_value, false);
    }

    @Override
    protected int computeLowNibble(int valuelen)
    {
        assert _hasNativeValue == true;

        int ln = 0;
        if (_decimal_value == null) {
            ln = IonConstants.lnIsNullAtom;
        }
        else {
            ln = getNativeValueLength();
            if (ln > IonConstants.lnIsVarLen) {
                // we get ln==VarLen for free, that is when len is 14 then
            	// it will fill in the VarLen anyway, but for a length that
            	// is greater ... we have to be explicit about VarLen
                ln = IonConstants.lnIsVarLen;
            }
        }
        return ln;
    }

    @Override
    protected void doMaterializeValue(IonBinary.Reader reader) throws IOException
    {
        assert this._isPositionLoaded == true && this._buffer != null;

        // a native value trumps a buffered value
        if (_hasNativeValue) return;

        // the reader will have been positioned for us
        assert reader.position() == this.pos_getOffsetAtValueTD();

        // we need to skip over the td to get to the good stuff
        int td = reader.read();
        assert (byte)(0xff & td) == this.pos_getTypeDescriptorByte();

        int type = this.pos_getType();
        if (type != IonConstants.tidDecimal) {
            throw new IonException("invalid type desc encountered for value");
        }

        int ln = this.pos_getLowNibble();
        switch ((0xf & ln)) {
        case IonConstants.lnIsNullAtom:
            _decimal_value = null;
            break;
        case 0:
            _decimal_value = Decimal.ZERO;
            break;
        case IonConstants.lnIsVarLen:
            ln = reader.readVarUInt7IntValue();
            // fall through to default:
        default:
            setValue(reader.readDecimalValue(ln));
            // setValue calls setDirty()
            // but materializing doesn't dirty the value - but no friend classes
            // and no struct's - so we have this hack instead :(
            super.setClean();
            break;
        }

        _hasNativeValue = true;
    }

    @Override
    protected void doWriteNakedValue(IonBinary.Writer writer, int valueLen) throws IOException
    {
        assert valueLen == this.getNakedValueLength();
        assert valueLen > 0;

        int wlen = writer.writeDecimalContent(_decimal_value, false);
        assert wlen == valueLen;

        return;
    }


    public void accept(ValueVisitor visitor) throws Exception
    {
        makeReady();
        visitor.visit(this);
    }
}
