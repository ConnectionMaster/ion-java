// Copyright (c) 2013 Amazon.com, Inc.  All rights reserved.

package com.amazon.ion;

import com.amazon.ion.IonValueDeltaGenerator.IonDecimalDeltaType;
import com.amazon.ion.IonValueDeltaGenerator.IonFloatDeltaType;
import com.amazon.ion.IonValueDeltaGenerator.IonIntDeltaType;
import com.amazon.ion.IonValueDeltaGenerator.IonSymbolDeltaType;
import com.amazon.ion.IonValueDeltaGenerator.IonTimestampDeltaType;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;


public class HashCodeDeltaCollisionTest
    extends IonTestCase
{
    private static final int NO_COLLISION = 0;
    private static final int DELTA_LIMIT = 1 << 4;

    /**
     * Generated non-deterministically by {@link #beforeClass()}.
     * Used by Timestamp delta collision test methods as a base millis value.
     */
    private static long TIMESTAMP_BASE_MILLIS;

    @BeforeClass
    public static void beforeClass()
    {
        long currentMillis = System.currentTimeMillis();
        long yearMillis = 60 * 60 * 24 * 365 * 1000L; // milliseconds in a year

        Random random = new Random(currentMillis);
        long randomRange = random.nextInt(100) * yearMillis; // 100 years range


        // TODO ION-324 Negative Epoch (with fractional precision) is failing
        // Timestamp internal validation.
        // range of TIMESTAMP_BASE_MILLIS is [-100, 100] from currentMillis
//        TIMESTAMP_BASE_MILLIS = random.nextBoolean()
//            ? currentMillis + randomRange
//            : currentMillis - randomRange;

        // range of TIMESTAMP_BASE_MILLIS is [0, 100] from currentMillis
        TIMESTAMP_BASE_MILLIS = currentMillis + randomRange;

        System.err.println(HashCodeDeltaCollisionTest.class.getSimpleName() +
                           ".TIMESTAMP_BASE_MILLIS=" +
                           TIMESTAMP_BASE_MILLIS + "L");
    }

    /**
     * Checks if the generated IonValues from the {@code generator} passes/fails
     * a hash code collision test.
     *
     * @param generator
     *          the generator of IonValues
     * @param limit
     *          the limit of collisions allowed, an {@link AssertionError} will
     *          be thrown if the limit is exceeded
     */
    protected void checkIonValueDeltaCollisions(IonValueDeltaGenerator generator,
                                                int limit)
    {
        // Set of hashcodes that are generated from IonValues
        Set<Integer> hashCodeSet = new HashSet<Integer>();
        Set<IonValue> values = generator.generateValues();
        int collisions = 0;

        for (IonValue value : values)
        {
            boolean collision = !hashCodeSet.add(value.hashCode());
            if (collision)
            {
                collisions++;
            }
        }

        if (collisions > limit)
        {
            fail("checkIonValueDeltaCollisions failed on " +
                 generator.getValueType() + "\n" +
                 " collisions: " + collisions +
                 " limit: "      + limit);
        }
    }

    @Test
    public void testIonIntLongDeltaCollisions() throws Exception
    {
        IonSystem ionSystem = system();
        IonInt baseInt = ionSystem.newInt(1337L); // IonInt is long-backed.
        IonIntDeltaType deltaType = IonIntDeltaType.LONG;

        for (int delta = 1; delta < DELTA_LIMIT; delta <<= 1)
        {
            IonValueDeltaGenerator generator =
                new IonValueDeltaGenerator.Builder()
                    .ionSystem(ionSystem)
                    .delta(delta)
                    .size(100000)
                    .baseValue(baseInt)
                    .deltaType(deltaType)
                    .build();

            checkIonValueDeltaCollisions(generator, NO_COLLISION);
        }
    }

    @Test
    public void testIonIntBigIntegerDeltaCollisions() throws Exception
    {
        IonSystem ionSystem = system();
        // Create a BigDecimal that exceeds range of Long, so IonInt is forcibly
        // BigInteger-backed.
        BigDecimal bigDec = BigDecimal.valueOf(Long.MAX_VALUE + 1337e10);
        IonInt baseInt = ionSystem.newInt(bigDec);
        IonIntDeltaType deltaType = IonIntDeltaType.BIGINTEGER;

        for (int delta = 1; delta < DELTA_LIMIT; delta <<= 1)
        {
            IonValueDeltaGenerator generator =
                new IonValueDeltaGenerator.Builder()
                    .ionSystem(ionSystem)
                    .delta(delta)
                    .size(100000)
                    .baseValue(baseInt)
                    .deltaType(deltaType)
                    .build();

            checkIonValueDeltaCollisions(generator, NO_COLLISION);
        }
    }

    @Test
    public void testIonFloatDeltaCollisions() throws Exception
    {
        IonSystem ionSystem = system();
        IonFloat baseFloat = ionSystem.newFloat(1337.1337d);

        for (IonFloatDeltaType deltaType : IonFloatDeltaType.values())
        {
            for (int delta = 1; delta < DELTA_LIMIT; delta <<= 1)
            {
                IonValueDeltaGenerator generator =
                    new IonValueDeltaGenerator.Builder()
                        .ionSystem(ionSystem)
                        .delta(delta)
                        .size(100000)
                        .baseValue(baseFloat)
                        .deltaType(deltaType)
                        .build();

                checkIonValueDeltaCollisions(generator, NO_COLLISION);
            }
        }
    }

    @Test
    public void testIonDecimalDeltaCollisions() throws Exception
    {
        IonSystem ionSystem = system();
        IonDecimal baseDecimal = ionSystem
            .newDecimal(BigDecimal.valueOf(1337.1337d));

        for (IonDecimalDeltaType deltaType : IonDecimalDeltaType.values())
        {
            for (int delta = 1; delta < DELTA_LIMIT; delta <<= 1)
            {
                IonValueDeltaGenerator generator =
                    new IonValueDeltaGenerator.Builder()
                        .ionSystem(ionSystem)
                        .delta(delta)
                        .size(10000)
                        .baseValue(baseDecimal)
                        .deltaType(deltaType)
                        .build();

                checkIonValueDeltaCollisions(generator, 5);
            }
        }
    }

    @Test
    public void testIonTimestampDeltaCollisions() throws Exception
    {
        IonSystem ionSystem = system();
        IonTimestamp baseTimestamp = ionSystem
            .newTimestamp(new Timestamp(TIMESTAMP_BASE_MILLIS, Timestamp.UTC_OFFSET));

        for (IonTimestampDeltaType deltaType : IonTimestampDeltaType.values())
        {
            for (int delta = 1; delta < DELTA_LIMIT; delta <<= 1)
            {
                IonValueDeltaGenerator generator =
                    new IonValueDeltaGenerator.Builder()
                        .ionSystem(ionSystem)
                        .delta(delta)
                        .size(10000)
                        .baseValue(baseTimestamp)
                        .deltaType(deltaType)
                        .build();

                checkIonValueDeltaCollisions(generator, NO_COLLISION);
            }
        }
    }

    @Test
    public void testIonSymbolSidDeltaCollisions() throws Exception
    {
        IonSystem ionSystem = system();
        IonSymbol baseSymbol = ionSystem
            .newSymbol(new FakeSymbolToken(null, 1337));

        IonSymbolDeltaType deltaType = IonSymbolDeltaType.SID;

        for (int delta = 1; delta < DELTA_LIMIT; delta <<= 1)
        {
            IonValueDeltaGenerator generator =
                new IonValueDeltaGenerator.Builder()
                    .ionSystem(ionSystem)
                    .delta(delta)
                    .size(100000)
                    .baseValue(baseSymbol)
                    .deltaType(deltaType)
                    .build();

            checkIonValueDeltaCollisions(generator, NO_COLLISION);
        }
    }

}