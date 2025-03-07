/*
 * Copyright (c) 2004-2021, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.expression;

import static java.util.Collections.singletonList;
import static org.hisp.dhis.analytics.DataType.BOOLEAN;
import static org.hisp.dhis.analytics.DataType.TEXT;
import static org.hisp.dhis.common.ReportingRateMetric.ACTUAL_REPORTS;
import static org.hisp.dhis.common.ReportingRateMetric.ACTUAL_REPORTS_ON_TIME;
import static org.hisp.dhis.common.ReportingRateMetric.EXPECTED_REPORTS;
import static org.hisp.dhis.common.ReportingRateMetric.REPORTING_RATE;
import static org.hisp.dhis.common.ReportingRateMetric.REPORTING_RATE_ON_TIME;
import static org.hisp.dhis.expression.ExpressionValidationOutcome.*;
import static org.hisp.dhis.expression.MissingValueStrategy.NEVER_SKIP;
import static org.hisp.dhis.expression.MissingValueStrategy.SKIP_IF_ALL_VALUES_MISSING;
import static org.hisp.dhis.expression.MissingValueStrategy.SKIP_IF_ANY_VALUE_MISSING;
import static org.hisp.dhis.expression.ParseType.*;
import static org.hisp.dhis.utils.Assertions.assertMapEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.*;
import java.util.stream.Collectors;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.antlr.ParserException;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.*;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.indicator.IndicatorValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * @author Jim Grace
 */
public class ExpressionServiceTest
    extends DhisSpringTest
{
    @Autowired
    private ExpressionService expressionService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private IndicatorService indicatorService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private ConstantService constantService;

    private OrganisationUnit orgUnitA;

    private OrganisationUnit orgUnitB;

    private OrganisationUnit orgUnitC;

    private OrganisationUnit orgUnitD;

    private OrganisationUnit orgUnitE;

    private OrganisationUnit orgUnitF;

    private OrganisationUnit orgUnitG;

    private OrganisationUnit orgUnitH;

    private OrganisationUnit orgUnitI;

    private OrganisationUnit orgUnitJ;

    private OrganisationUnit orgUnitK;

    private OrganisationUnit orgUnitL;

    private OrganisationUnitGroup orgUnitGroupA;

    private OrganisationUnitGroup orgUnitGroupB;

    private OrganisationUnitGroup orgUnitGroupC;

    private DataSet dataSetA;

    private DataSet dataSetB;

    private static DataElement dataElementA;

    private static DataElement dataElementB;

    private static DataElement dataElementC;

    private static DataElement dataElementD;

    private static DataElement dataElementE;

    private static DataElement dataElementF;

    private static DataElement dataElementG;

    private static DataElement dataElementH;

    private static IndicatorType indicatorTypeB;

    private static Indicator indicatorA;

    private static CategoryOption categoryOptionA;

    private static CategoryOption categoryOptionB;

    private static Category categoryA;

    private static CategoryCombo categoryComboA;

    private static CategoryOptionCombo categoryOptionComboA;

    private static CategoryOptionCombo categoryOptionComboB;

    private static DataElementOperand dataElementOperandA;

    private static DataElementOperand dataElementOperandB;

    private static DataElementOperand dataElementOperandC;

    private static DataElementOperand dataElementOperandD;

    private static DataElementOperand dataElementOperandE;

    private static DataElementOperand dataElementOperandF;

    private static ProgramDataElementDimensionItem programDataElementA;

    private static ProgramDataElementDimensionItem programDataElementB;

    private static Program programA;

    private static Program programB;

    private static ProgramIndicator programIndicatorA;

    private static ProgramIndicator programIndicatorB;

    private static TrackedEntityAttribute trackedEntityAttributeA;

    private static TrackedEntityAttribute trackedEntityAttributeB;

    private static ProgramTrackedEntityAttributeDimensionItem programAttributeA;

    private static ProgramTrackedEntityAttributeDimensionItem programAttributeB;

    private static ReportingRate reportingRateA;

    private static ReportingRate reportingRateB;

    private static ReportingRate reportingRateC;

    private static ReportingRate reportingRateD;

    private static ReportingRate reportingRateE;

    private static ReportingRate reportingRateF;

    private static IndicatorType indicatorTypeA;

    private Map<DimensionalItemObject, Object> valueMap;

    private MapMap<Period, DimensionalItemObject, Object> samples;

    private Map<String, Constant> constantMap;

    private static final Map<String, Integer> ORG_UNIT_COUNT_MAP = new ImmutableMap.Builder<String, Integer>()
        .put( "orgUnitGrpA", 1000000 )
        .put( "orgUnitGrpB", 2000000 )
        .build();

    private static final Period samplePeriod1 = PeriodType.getPeriodFromIsoString( "20200101" );

    private static final Period samplePeriod2 = PeriodType.getPeriodFromIsoString( "20200102" );

    private static final List<Period> TEST_SAMPLE_PERIODS = Lists.newArrayList( samplePeriod1, samplePeriod2 );

    private final static int DAYS = 30;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
        throws Exception
    {
        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );
        dataElementC = createDataElement( 'C' );
        dataElementD = createDataElement( 'D' );
        dataElementE = createDataElement( 'E' );
        dataElementF = createDataElement( 'F' );
        dataElementG = createDataElement( 'G' );
        dataElementH = createDataElement( 'H' );

        dataElementA.setUid( "dataElemenA" );
        dataElementB.setUid( "dataElemenB" );
        dataElementC.setUid( "dataElemenC" );
        dataElementD.setUid( "dataElemenD" );
        dataElementE.setUid( "dataElemenE" );
        dataElementF.setUid( "dataElemenF" );
        dataElementG.setUid( "dataElemenG" );
        dataElementH.setUid( "dataElemenH" );

        dataElementA.setAggregationType( AggregationType.SUM );
        dataElementB.setAggregationType( AggregationType.NONE );
        dataElementC.setAggregationType( AggregationType.SUM );
        dataElementD.setAggregationType( AggregationType.NONE );
        dataElementE.setAggregationType( AggregationType.SUM );
        dataElementF.setAggregationType( AggregationType.NONE );
        dataElementG.setAggregationType( AggregationType.NONE );
        dataElementH.setAggregationType( AggregationType.NONE );

        dataElementF.setValueType( ValueType.TEXT );
        dataElementG.setValueType( ValueType.DATE );
        dataElementH.setValueType( ValueType.BOOLEAN );

        dataElementC.setDomainType( DataElementDomain.TRACKER );
        dataElementD.setDomainType( DataElementDomain.TRACKER );

        dataElementA.setName( "DeA" );
        dataElementB.setName( "DeB" );
        dataElementC.setName( "DeC" );
        dataElementD.setName( "DeD" );
        dataElementE.setName( "DeE" );
        dataElementF.setName( "DeF" );
        dataElementG.setName( "DeG" );
        dataElementH.setName( "DeH" );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );
        dataElementService.addDataElement( dataElementE );
        dataElementService.addDataElement( dataElementF );
        dataElementService.addDataElement( dataElementG );
        dataElementService.addDataElement( dataElementH );

        indicatorTypeB = createIndicatorType( 'B' );
        indicatorService.addIndicatorType( indicatorTypeB );

        indicatorA = createIndicator( 'A', indicatorTypeB );
        indicatorA.setUid( "mindicatorA" );

        indicatorService.addIndicator( indicatorA );

        categoryOptionA = createCategoryOption( 'A' );
        categoryOptionB = createCategoryOption( 'B' );

        categoryService.addCategoryOption( categoryOptionA );
        categoryService.addCategoryOption( categoryOptionB );

        categoryA = createCategory( 'A', categoryOptionA, categoryOptionB );

        categoryService.addCategory( categoryA );

        categoryComboA = createCategoryCombo( 'A', categoryA );

        categoryService.addCategoryCombo( categoryComboA );

        categoryOptionComboA = createCategoryOptionCombo( categoryComboA, categoryOptionA );
        categoryOptionComboB = createCategoryOptionCombo( categoryComboA, categoryOptionB );

        categoryOptionComboA.setUid( "catOptCombA" );
        categoryOptionComboB.setUid( "catOptCombB" );

        categoryOptionComboA.setName( "CocA" );
        categoryOptionComboB.setName( "CocB" );

        categoryService.addCategoryOptionCombo( categoryOptionComboA );
        categoryService.addCategoryOptionCombo( categoryOptionComboB );

        dataElementOperandA = new DataElementOperand( dataElementA, categoryOptionComboB );
        dataElementOperandB = new DataElementOperand( dataElementB, categoryOptionComboA );
        dataElementOperandC = new DataElementOperand( dataElementA, categoryOptionComboA, categoryOptionComboB );
        dataElementOperandD = new DataElementOperand( dataElementB, categoryOptionComboB, categoryOptionComboA );
        dataElementOperandE = new DataElementOperand( dataElementA, null, categoryOptionComboB );
        dataElementOperandF = new DataElementOperand( dataElementB, null, categoryOptionComboA );

        programA = createProgram( 'A' );
        programB = createProgram( 'B' );

        programA.setUid( "programUidA" );
        programB.setUid( "programUidB" );

        programA.setName( "PA" );
        programB.setName( "PB" );

        manager.save( programA );
        manager.save( programB );

        programDataElementA = new ProgramDataElementDimensionItem( programA, dataElementC );
        programDataElementB = new ProgramDataElementDimensionItem( programB, dataElementD );

        trackedEntityAttributeA = createTrackedEntityAttribute( 'A', ValueType.NUMBER );
        trackedEntityAttributeB = createTrackedEntityAttribute( 'B', ValueType.NUMBER );

        trackedEntityAttributeA.setUid( "trakEntAttA" );
        trackedEntityAttributeB.setUid( "trakEntAttB" );

        trackedEntityAttributeA.setName( "TeaA" );
        trackedEntityAttributeB.setName( "TeaB" );

        trackedEntityAttributeA.setAggregationType( AggregationType.SUM );
        trackedEntityAttributeB.setAggregationType( AggregationType.NONE );

        manager.save( trackedEntityAttributeA );
        manager.save( trackedEntityAttributeB );

        programAttributeA = new ProgramTrackedEntityAttributeDimensionItem( programA, trackedEntityAttributeA );
        programAttributeB = new ProgramTrackedEntityAttributeDimensionItem( programB, trackedEntityAttributeB );

        programIndicatorA = createProgramIndicator( 'A', programA, "9.0", "" );
        programIndicatorB = createProgramIndicator( 'B', programA, "19.0", "" );

        programIndicatorA.setUid( "programIndA" );
        programIndicatorB.setUid( "programIndB" );

        programIndicatorA.setName( "PiA" );
        programIndicatorB.setName( "PiB" );

        programIndicatorA.setAggregationType( AggregationType.SUM );
        programIndicatorB.setAggregationType( AggregationType.NONE );

        manager.save( programIndicatorA );
        manager.save( programIndicatorB );

        orgUnitA = createOrganisationUnit( 'A' );
        orgUnitB = createOrganisationUnit( 'B', orgUnitA );
        orgUnitC = createOrganisationUnit( 'C', orgUnitA );
        orgUnitD = createOrganisationUnit( 'D', orgUnitA );
        orgUnitE = createOrganisationUnit( 'E', orgUnitB );
        orgUnitF = createOrganisationUnit( 'F', orgUnitC );
        orgUnitG = createOrganisationUnit( 'G', orgUnitC );
        orgUnitH = createOrganisationUnit( 'H', orgUnitC );
        orgUnitI = createOrganisationUnit( 'I', orgUnitD );
        orgUnitJ = createOrganisationUnit( 'J', orgUnitG );
        orgUnitK = createOrganisationUnit( 'K', orgUnitG );
        orgUnitL = createOrganisationUnit( 'L', orgUnitJ );

        orgUnitA.setUid( "OrgUnitUidA" );
        orgUnitB.setUid( "OrgUnitUidB" );
        orgUnitC.setUid( "OrgUnitUidC" );
        orgUnitD.setUid( "OrgUnitUidD" );
        orgUnitE.setUid( "OrgUnitUidE" );
        orgUnitF.setUid( "OrgUnitUidF" );
        orgUnitG.setUid( "OrgUnitUidG" );
        orgUnitH.setUid( "OrgUnitUidH" );
        orgUnitI.setUid( "OrgUnitUidI" );
        orgUnitJ.setUid( "OrgUnitUidJ" );
        orgUnitK.setUid( "OrgUnitUidK" );
        orgUnitL.setUid( "OrgUnitUidL" );

        orgUnitA.setName( "OuA" );
        orgUnitB.setName( "OuB" );
        orgUnitC.setName( "OuC" );
        orgUnitD.setName( "OuD" );
        orgUnitE.setName( "OuE" );
        orgUnitF.setName( "OuF" );
        orgUnitG.setName( "OuG" );
        orgUnitH.setName( "OuH" );
        orgUnitI.setName( "OuI" );
        orgUnitJ.setName( "OuJ" );
        orgUnitK.setName( "OuK" );
        orgUnitL.setName( "OuL" );

        organisationUnitService.addOrganisationUnit( orgUnitA );
        organisationUnitService.addOrganisationUnit( orgUnitB );
        organisationUnitService.addOrganisationUnit( orgUnitC );
        organisationUnitService.addOrganisationUnit( orgUnitD );
        organisationUnitService.addOrganisationUnit( orgUnitE );
        organisationUnitService.addOrganisationUnit( orgUnitF );
        organisationUnitService.addOrganisationUnit( orgUnitG );
        organisationUnitService.addOrganisationUnit( orgUnitH );
        organisationUnitService.addOrganisationUnit( orgUnitI );
        organisationUnitService.addOrganisationUnit( orgUnitJ );
        organisationUnitService.addOrganisationUnit( orgUnitK );
        organisationUnitService.addOrganisationUnit( orgUnitL );

        orgUnitGroupA = createOrganisationUnitGroup( 'A' );
        orgUnitGroupB = createOrganisationUnitGroup( 'B' );
        orgUnitGroupC = createOrganisationUnitGroup( 'C' );

        orgUnitGroupA.setUid( "orgUnitGrpA" );
        orgUnitGroupB.setUid( "orgUnitGrpB" );
        orgUnitGroupC.setUid( "orgUnitGrpC" );

        orgUnitGroupA.setCode( "orgUnitGroupCodeA" );
        orgUnitGroupB.setCode( "orgUnitGroupCodeB" );
        orgUnitGroupC.setCode( "orgUnitGroupCodeC" );

        orgUnitGroupA.setName( "OugA" );
        orgUnitGroupB.setName( "OugB" );
        orgUnitGroupC.setName( "OugC" );

        orgUnitGroupA.addOrganisationUnit( orgUnitB );
        orgUnitGroupA.addOrganisationUnit( orgUnitC );
        orgUnitGroupA.addOrganisationUnit( orgUnitE );
        orgUnitGroupA.addOrganisationUnit( orgUnitF );
        orgUnitGroupA.addOrganisationUnit( orgUnitG );

        orgUnitGroupB.addOrganisationUnit( orgUnitF );
        orgUnitGroupB.addOrganisationUnit( orgUnitG );
        orgUnitGroupB.addOrganisationUnit( orgUnitH );

        orgUnitGroupC.addOrganisationUnit( orgUnitC );
        orgUnitGroupC.addOrganisationUnit( orgUnitD );
        orgUnitGroupC.addOrganisationUnit( orgUnitG );
        orgUnitGroupC.addOrganisationUnit( orgUnitH );
        orgUnitGroupC.addOrganisationUnit( orgUnitI );

        organisationUnitGroupService.addOrganisationUnitGroup( orgUnitGroupA );
        organisationUnitGroupService.addOrganisationUnitGroup( orgUnitGroupB );
        organisationUnitGroupService.addOrganisationUnitGroup( orgUnitGroupC );

        dataSetA = createDataSet( 'A' );
        dataSetB = createDataSet( 'B' );

        dataSetA.setUid( "dataSetUidA" );
        dataSetB.setUid( "dataSetUidB" );

        dataSetA.setName( "DsA" );
        dataSetB.setName( "DsB" );

        dataSetA.setCode( "dataSetCodeA" );
        dataSetB.setCode( "dataSetCodeB" );

        dataSetA.addOrganisationUnit( orgUnitE );
        dataSetA.addOrganisationUnit( orgUnitH );
        dataSetA.addOrganisationUnit( orgUnitI );

        dataSetB.addOrganisationUnit( orgUnitF );
        dataSetB.addOrganisationUnit( orgUnitG );
        dataSetB.addOrganisationUnit( orgUnitI );

        dataSetService.addDataSet( dataSetA );
        dataSetService.addDataSet( dataSetB );

        reportingRateA = new ReportingRate( dataSetA, REPORTING_RATE );
        reportingRateB = new ReportingRate( dataSetA, REPORTING_RATE_ON_TIME );
        reportingRateC = new ReportingRate( dataSetA, ACTUAL_REPORTS );
        reportingRateD = new ReportingRate( dataSetA, ACTUAL_REPORTS_ON_TIME );
        reportingRateE = new ReportingRate( dataSetA, EXPECTED_REPORTS );
        reportingRateF = new ReportingRate( dataSetB );

        indicatorTypeA = new IndicatorType( "A", 100, false );

        Constant constantA = new Constant( "One half", 0.5 );
        Constant constantB = new Constant( "One quarter", 0.25 );

        constantA.setUid( "xxxxxxxxx05" );
        constantB.setUid( "xxxxxxxx025" );

        constantService.saveConstant( constantA );
        constantService.saveConstant( constantB );

        constantMap = constantService.getConstantMap();

        valueMap = new ImmutableMap.Builder<DimensionalItemObject, Object>()

            .put( dataElementA, 3.0 )
            .put( dataElementB, 13.0 )
            .put( dataElementF, "Str" )
            .put( dataElementG, "2022-01-15" )
            .put( dataElementH, true )

            .put( dataElementOperandA, 5.0 )
            .put( dataElementOperandB, 15.0 )
            .put( dataElementOperandC, 7.0 )
            .put( dataElementOperandD, 17.0 )
            .put( dataElementOperandE, 9.0 )
            .put( dataElementOperandF, 19.0 )

            .put( programDataElementA, 101.0 )
            .put( programDataElementB, 102.0 )

            .put( programAttributeA, 201.0 )
            .put( programAttributeB, 202.0 )

            .put( programIndicatorA, 301.0 )
            .put( programIndicatorB, 302.0 )

            .put( reportingRateA, 401.0 )
            .put( reportingRateB, 402.0 )
            .put( reportingRateC, 403.0 )
            .put( reportingRateD, 404.0 )
            .put( reportingRateE, 405.0 )
            .put( reportingRateF, 406.0 )

            .put( indicatorA, 88.0 )

            .build();

        samples = new MapMap<>();

        samples.putEntries( samplePeriod1, new ImmutableMap.Builder<DimensionalItemObject, Object>()
            .put( dataElementC, 2.0 )
            .build() );

        samples.putEntries( samplePeriod2, new ImmutableMap.Builder<DimensionalItemObject, Object>()
            .put( dataElementB, 1.0 )
            .put( dataElementC, 3.0 )
            .build() );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Evaluates a test expression, against getExpressionDimensionalItemObjects
     * and getExpressionValue. Returns a string containing first the returned
     * value from getExpressionValue, and then the items returned from
     * getExpressionDimensionalItemObjects, if any, separated by spaces.
     *
     * @param expr expression to evaluate
     * @param parseType type of expression to parse
     * @param missingValueStrategy strategy to use if item value is missing
     * @param dataType data type that the expression should return
     * @return result from testing the expression
     */
    private String eval( String expr, ParseType parseType,
        MissingValueStrategy missingValueStrategy, DataType dataType )
    {
        try
        {
            expressionService.getExpressionDescription( expr, parseType, dataType );
        }
        catch ( ParserException ex )
        {
            return ex.getMessage();
        }

        Map<DimensionalItemId, DimensionalItemObject> itemMap = new HashMap<>();

        expressionService.getExpressionDimensionalItemMaps( expr, parseType, dataType,
            itemMap, itemMap );

        Object value = expressionService.getExpressionValue( expr, parseType, itemMap, valueMap,
            constantMap, ORG_UNIT_COUNT_MAP, null, DAYS, missingValueStrategy,
            null, TEST_SAMPLE_PERIODS, samples, dataType );

        return result( value, itemMap.values() );
    }

    /**
     * Evaluates a test expression, against getExpressionDimensionalItemObjects
     * and getExpressionValue. Returns a string containing first the returned
     * value from getExpressionValue, and then the items returned from
     * getExpressionDimensionalItemObjects, if any, separated by spaces.
     *
     * @param expr expression to evaluate
     * @param missingValueStrategy strategy to use if item value is missing
     * @return result from testing the expression
     */
    private String eval( String expr, MissingValueStrategy missingValueStrategy )
    {
        return eval( expr, INDICATOR_EXPRESSION, missingValueStrategy, DataType.NUMERIC );
    }

    /**
     * Evaluates a test predictor numeric expression, against
     * getExpressionDimensionalItemObjects and getExpressionValue. Returns a
     * string containing first the returned value from getExpressionValue, and
     * then the items returned from getExpressionDimensionalItemObjects, if any,
     * separated by spaces.
     *
     * @param expr expression to evaluate
     * @param missingValueStrategy strategy to use if item value is missing
     * @return result from testing the expression
     */
    private String evalPredictor( String expr, MissingValueStrategy missingValueStrategy )
    {
        return eval( expr, PREDICTOR_EXPRESSION, missingValueStrategy, DataType.NUMERIC );
    }

    /**
     * Evaluates a test predictor expression of a given dataType, against
     * getExpressionDimensionalItemObjects and getExpressionValue. Returns a
     * string containing first the returned value from getExpressionValue, and
     * then the items returned from getExpressionDimensionalItemObjects, if any,
     * separated by spaces.
     *
     * @param expr expression to evaluate
     * @param dataType strategy to use if item value is missing
     * @return result from testing the expression
     */
    private String evalPredictor( String expr, DataType dataType )
    {
        return eval( expr, PREDICTOR_EXPRESSION, SKIP_IF_ANY_VALUE_MISSING, dataType );
    }

    /**
     * Evaluates a test expression, returns NULL if any values are missing.
     *
     * @param expr expression to evaluate
     * @return result from testing the expression
     */
    private String eval( String expr )
    {
        return eval( expr, SKIP_IF_ANY_VALUE_MISSING );
    }

    /**
     * Evaluates a test expression as a Double.
     *
     * @param expr expression to evaluate
     * @return result from testing the expression
     */
    private Double evalDouble( String expr )
    {
        return Double.parseDouble( eval( expr ) );
    }

    /**
     * Formats the result from testing the expression
     *
     * @param value the value retuned from getExpressionValueRegEx
     * @param items the items returned from getExpressionItems
     * @return the result string
     */
    private String result( Object value, Collection<DimensionalItemObject> items )
    {
        String valueString;

        if ( value == null )
        {
            valueString = "null";
        }
        else if ( value instanceof Double )
        {
            double d = (double) value;

            if ( d == (int) d )
            {
                valueString = Integer.toString( (int) d );
            }
            else
            {
                valueString = value.toString();
            }
        }
        else if ( value instanceof String )
        {
            valueString = "'" + value + "'";
        }
        else if ( value instanceof Boolean )
        {
            valueString = value.toString();
        }
        else
        {
            valueString = "Class " + value.getClass().getName() + " " + value.toString();
        }

        List<String> itemNames = items.stream().map( IdentifiableObject::getName ).sorted()
            .collect( Collectors.toList() );

        String itemsString = String.join( " ", itemNames );

        if ( itemsString.length() != 0 )
        {
            itemsString = " " + itemsString;
        }

        return valueString + itemsString;
    }

    /**
     * Make sure the expression causes an error
     *
     * @param expr The expression to test
     * @return null if error, otherwise expression description
     */
    private String error( String expr )
    {
        String description;

        try
        {
            description = expressionService.getExpressionDescription( expr, INDICATOR_EXPRESSION );
        }
        catch ( ParserException ex )
        {
            return null;
        }

        return "Unexpected success getting description: '" + expr + "' - '" + description + "'";
    }

    /**
     * Gets the organisation unit groups (if any) in an expression
     *
     * @param expr the expression string
     * @return a string with org unit gruop names (if any)
     */
    private String getOrgUnitGroups( String expr )
    {
        Set<OrganisationUnitGroup> orgUnitGroups = expressionService.getExpressionOrgUnitGroups( expr,
            PREDICTOR_EXPRESSION );

        List<String> orgUnitGroupNames = orgUnitGroups.stream()
            .map( BaseIdentifiableObject::getName )
            .sorted().collect( Collectors.toList() );

        return String.join( ", ", orgUnitGroupNames );
    }

    /**
     * Gets an expression description
     *
     * @param expr the expression string
     * @return the description
     */
    private String desc( String expr )
    {
        return expressionService.getExpressionDescription( expr, PREDICTOR_EXPRESSION );
    }

    /**
     * Checks the validity of an expresison
     *
     * @param expr the expression string
     * @param parseType type of expression to parse
     * @return the validation outcome
     */
    ExpressionValidationOutcome validity( String expr, ParseType parseType )
    {
        return expressionService.expressionIsValid( expr, parseType );
    }

    // -------------------------------------------------------------------------
    // Expression tests
    // -------------------------------------------------------------------------

    @Test
    public void testExpressionNumeric()
    {
        // Numeric constants

        assertEquals( "2", eval( "2" ) );
        assertEquals( "2", eval( "2." ) );
        assertEquals( "2", eval( "2.0" ) );
        assertEquals( "2.1", eval( "2.1" ) );
        assertEquals( "0.2", eval( "0.2" ) );
        assertEquals( "0.2", eval( ".2" ) );
        assertEquals( "2", eval( "2E0" ) );
        assertEquals( "2", eval( "2e0" ) );
        assertEquals( "2", eval( "2.E0" ) );
        assertEquals( "2", eval( "2.0E0" ) );
        assertEquals( "2.1", eval( "2.1E0" ) );
        assertEquals( "2.1", eval( "2.1E+0" ) );
        assertEquals( "2.1", eval( "2.1E-0" ) );
        assertEquals( "0.21", eval( "2.1E-1" ) );
        assertEquals( "0.021", eval( "2.1E-2" ) );
        assertEquals( "20", eval( "2E1" ) );
        assertEquals( "20", eval( "2E+1" ) );
        assertEquals( "20", eval( "2E01" ) );
        assertEquals( "200", eval( "2E2" ) );
        assertEquals( "2", eval( "+2" ) );
        assertEquals( "-2", eval( "-2" ) );

        // Numeric operators in precedence order:

        // Exponentiation (right-to-left)

        assertEquals( "512", eval( "2 ^ 3 ^ 2" ) );
        assertEquals( "64", eval( "( 2 ^ 3 ) ^ 2" ) );
        assertEquals( "0.25", eval( "2 ^ -2" ) );

        assertEquals( "null DeA DeE", eval( "#{dataElemenA} ^ #{dataElemenE}" ) );
        assertEquals( "null DeA DeE", eval( "#{dataElemenE} ^ #{dataElemenA}" ) );

        // Unary +, -

        assertEquals( "5", eval( "+ (2 + 3)" ) );
        assertEquals( "-5", eval( "- (2 + 3)" ) );

        assertEquals( "null DeE", eval( "- #{dataElemenE}" ) );

        // Unary +, - after Exponentiation

        assertEquals( "-4", eval( "-(2) ^ 2" ) );
        assertEquals( "4", eval( "(-(2)) ^ 2" ) );
        assertEquals( "4", eval( "+(2) ^ 2" ) );

        // Multiply, divide, modulus (left-to-right)

        assertEquals( "24", eval( "2 * 3 * 4" ) );
        assertEquals( "2", eval( "12 / 3 / 2" ) );
        assertEquals( "8", eval( "12 / ( 3 / 2 )" ) );
        assertEquals( "2", eval( "12 % 5 % 3" ) );
        assertEquals( "0", eval( "12 % ( 5 % 3 )" ) );
        assertEquals( "8", eval( "12 / 3 * 2" ) );
        assertEquals( "2", eval( "12 / ( 3 * 2 )" ) );
        assertEquals( "3", eval( "5 % 2 * 3" ) );
        assertEquals( "1", eval( "3 * 5 % 2" ) );
        assertEquals( "1.5", eval( "7 % 4 / 2" ) );
        assertEquals( "1", eval( "9 / 3 % 2" ) );

        assertEquals( "null DeA DeE", eval( "#{dataElemenA} * #{dataElemenE}" ) );
        assertEquals( "null DeA DeE", eval( "#{dataElemenE} / #{dataElemenA}" ) );
        assertEquals( "null DeA DeE", eval( "#{dataElemenA} % #{dataElemenE}" ) );

        // Multiply, divide, modulus after Unary +, -

        assertEquals( "-6", eval( "-(3) * 2" ) );
        assertEquals( "-6", eval( "-(3 * 2)" ) );
        assertEquals( "-1.5", eval( "-(3) / 2" ) );
        assertEquals( "-1.5", eval( "-(3 / 2)" ) );
        assertEquals( "-1", eval( "-(7) % 3" ) );
        assertEquals( "-1", eval( "-(7 % 3)" ) );

        // Add, subtract (left-to-right)

        assertEquals( "9", eval( "2 + 3 + 4" ) );
        assertEquals( "9", eval( "2 + ( 3 + 4 )" ) );
        assertEquals( "-5", eval( "2 - 3 - 4" ) );
        assertEquals( "3", eval( "2 - ( 3 - 4 )" ) );
        assertEquals( "3", eval( "2 - 3 + 4" ) );
        assertEquals( "-5", eval( "2 - ( 3 + 4 )" ) );

        assertEquals( "null DeA DeE", eval( "#{dataElemenA} + #{dataElemenE}" ) );
        assertEquals( "null DeA DeE", eval( "#{dataElemenE} - #{dataElemenA}" ) );

        // Add, subtract after Multiply, divide, modulus

        assertEquals( "10", eval( "4 + 3 * 2" ) );
        assertEquals( "14", eval( "( 4 + 3 ) * 2" ) );
        assertEquals( "5.5", eval( "4 + 3 / 2" ) );
        assertEquals( "3.5", eval( "( 4 + 3 ) / 2" ) );
        assertEquals( "5", eval( "4 + 3 % 2" ) );
        assertEquals( "1", eval( "( 4 + 3 ) % 2" ) );

        assertEquals( "-2", eval( "4 - 3 * 2" ) );
        assertEquals( "2", eval( "( 4 - 3 ) * 2" ) );
        assertEquals( "2.5", eval( "4 - 3 / 2" ) );
        assertEquals( "0.5", eval( "( 4 - 3 ) / 2" ) );
        assertEquals( "3", eval( "4 - 3 % 2" ) );
        assertEquals( "1", eval( "( 4 - 3 ) % 2" ) );

        // Logarithms

        assertEquals( 3.912023005428146, evalDouble( "log(50)" ), DELTA );
        assertEquals( 1, evalDouble( "log(2.718281828459045)" ), DELTA );
        assertEquals( "-Infinity", eval( "log(0)" ) );
        assertEquals( "NaN", eval( "log(-1)" ) );

        assertEquals( 3.5608767950073115, evalDouble( "log(50,3)" ), DELTA );
        assertEquals( 3, evalDouble( "log(8,2)" ), DELTA );
        assertEquals( "-Infinity", eval( "log(0,3)" ) );
        assertEquals( "NaN", eval( "log(-1,3)" ) );
        assertEquals( "0", eval( "log(50,0)" ) );
        assertEquals( "NaN", eval( "log(50,-3)" ) );
        assertEquals( "NaN", eval( "log(-50,-3)" ) );

        assertEquals( 1.6989700043360187, evalDouble( "log10(50)" ), DELTA );
        assertEquals( 3, evalDouble( "log10(1000)" ), DELTA );
        assertEquals( "-Infinity", eval( "log10(0)" ) );
        assertEquals( "NaN", eval( "log10(-1)" ) );

        // Comparisons (left-to-right)

        assertEquals( "1", eval( "if(1 < 2, 1, 0)" ) );
        assertEquals( "0", eval( "if(1 < 1, 1, 0)" ) );
        assertEquals( "0", eval( "if(2 < 1, 1, 0)" ) );

        assertEquals( "0", eval( "if(1 > 2, 1, 0)" ) );
        assertEquals( "0", eval( "if(1 > 1, 1, 0)" ) );
        assertEquals( "1", eval( "if(2 > 1, 1, 0)" ) );

        assertEquals( "1", eval( "if(1 <= 2, 1, 0)" ) );
        assertEquals( "1", eval( "if(1 <= 1, 1, 0)" ) );
        assertEquals( "0", eval( "if(2 <= 1, 1, 0)" ) );

        assertEquals( "0", eval( "if(1 >= 2, 1, 0)" ) );
        assertEquals( "1", eval( "if(1 >= 1, 1, 0)" ) );
        assertEquals( "1", eval( "if(2 >= 1, 1, 0)" ) );

        assertEquals( "null DeA DeE", eval( "if( #{dataElemenA} > #{dataElemenE}, 1, 0)" ) );
        assertEquals( "null DeA DeE", eval( "if( #{dataElemenE} < #{dataElemenA}, 1, 0)" ) );

        // Comparisons after Add, subtract

        assertEquals( "0", eval( "if(5 < 2 + 3, 1, 0)" ) );
        assertEquals( "0", eval( "if(5 > 2 + 3, 1, 0)" ) );
        assertEquals( "1", eval( "if(5 <= 2 + 3, 1, 0)" ) );
        assertEquals( "1", eval( "if(5 >= 2 + 3, 1, 0)" ) );

        assertEquals( "0", eval( "if(5 < 8 - 3, 1, 0)" ) );
        assertEquals( "0", eval( "if(5 > 8 - 3, 1, 0)" ) );
        assertEquals( "1", eval( "if(5 <= 8 - 3, 1, 0)" ) );
        assertEquals( "1", eval( "if(5 >= 8 - 3, 1, 0)" ) );

        assertNull( error( "if((5 < 2) + 3, 1, 0)" ) );
        assertNull( error( "if((5 > 2) + 3, 1, 0)" ) );
        assertNull( error( "if((5 <= 2) + 3, 1, 0)" ) );
        assertNull( error( "if((5 >= 2) + 3, 1, 0)" ) );

        assertNull( error( "if((5 < 8) - 3, 1, 0)" ) );
        assertNull( error( "if((5 > 8) - 3, 1, 0)" ) );
        assertNull( error( "if((5 <= 8) - 3, 1, 0)" ) );
        assertNull( error( "if((5 >= 8) - 3, 1, 0)" ) );

        // Equality

        assertEquals( "1", eval( "if(1 == 1, 1, 0)" ) );
        assertEquals( "0", eval( "if(1 == 2, 1, 0)" ) );

        assertEquals( "0", eval( "if(1 != 1, 1, 0)" ) );
        assertEquals( "1", eval( "if(1 != 2, 1, 0)" ) );

        assertEquals( "null DeA DeE", eval( "if( #{dataElemenA} == #{dataElemenE}, 1, 0)" ) );
        assertEquals( "null DeA DeE", eval( "if( #{dataElemenE} != #{dataElemenA}, 1, 0)" ) );

        // Equality after Comparisons

        assertEquals( "1", eval( "if(1 + 2 == 3, 1, 0)" ) );
        assertEquals( "0", eval( "if(1 + 2 != 3, 1, 0)" ) );

        assertNull( error( "if(1 + (2 == 3), 1, 0)" ) );
        assertNull( error( "if(1 + (2 != 3), 1, 0)" ) );
    }

    @Test
    public void testExpressionString()
    {
        // Comparisons

        assertEquals( "0", eval( "if( 'a' < 'a', 1, 0)" ) );
        assertEquals( "1", eval( "if( 'a' < 'b', 1, 0)" ) );
        assertEquals( "0", eval( "if( 'b' < 'a', 1, 0)" ) );

        assertEquals( "0", eval( "if( 'a' > 'a', 1, 0)" ) );
        assertEquals( "0", eval( "if( 'a' > 'b', 1, 0)" ) );
        assertEquals( "1", eval( "if( 'b' > 'a', 1, 0)" ) );

        assertEquals( "1", eval( "if( 'a' <= 'a', 1, 0)" ) );
        assertEquals( "1", eval( "if( 'a' <= 'b', 1, 0)" ) );
        assertEquals( "0", eval( "if( 'b' <= 'a', 1, 0)" ) );

        assertEquals( "1", eval( "if( 'a' >= 'a', 1, 0)" ) );
        assertEquals( "0", eval( "if( 'a' >= 'b', 1, 0)" ) );
        assertEquals( "1", eval( "if( 'b' >= 'a', 1, 0)" ) );

        // Equality

        assertEquals( "1", eval( "if( 'a' == 'a', 1, 0)" ) );
        assertEquals( "0", eval( "if( 'a' == 'b', 1, 0)" ) );

        assertEquals( "0", eval( "if( 'a' != 'a', 1, 0)" ) );
        assertEquals( "1", eval( "if( 'a' != 'b', 1, 0)" ) );
    }

    @Test
    public void testExpressionBoolean()
    {
        // Boolean constants

        assertEquals( "1", eval( "if( true, 1, 0)" ) );
        assertEquals( "0", eval( "if( false, 1, 0)" ) );

        // Unary not

        assertEquals( "0", eval( "if( ! true, 1, 0)" ) );
        assertEquals( "1", eval( "if( ! false, 1, 0)" ) );

        assertEquals( "null DeA DeE", eval( "if( ! (#{dataElemenA} == #{dataElemenE}), 1, 0)" ) );

        // Unary not before comparison

        assertNull( error( "if( ! 5 > 3, 1, 0)" ) );
        assertEquals( "0", eval( "if( ! (5 > 3), 1, 0)" ) );

        // Comparison

        assertEquals( "0", eval( "if( true < true, 1, 0)" ) );
        assertEquals( "0", eval( "if( true < false, 1, 0)" ) );
        assertEquals( "1", eval( "if( false < true, 1, 0)" ) );

        assertEquals( "0", eval( "if( true > true, 1, 0)" ) );
        assertEquals( "1", eval( "if( true > false, 1, 0)" ) );
        assertEquals( "0", eval( "if( false > true, 1, 0)" ) );

        assertEquals( "1", eval( "if( true <= true, 1, 0)" ) );
        assertEquals( "0", eval( "if( true <= false, 1, 0)" ) );
        assertEquals( "1", eval( "if( false <= true, 1, 0)" ) );

        assertEquals( "1", eval( "if( true >= true, 1, 0)" ) );
        assertEquals( "1", eval( "if( true >= false, 1, 0)" ) );
        assertEquals( "0", eval( "if( false >= true, 1, 0)" ) );

        // Comparison after Unary not

        assertEquals( "0", eval( "if( ! true < false, 1, 0)" ) );
        assertEquals( "0", eval( "if( ! true > false, 1, 0)" ) );
        assertEquals( "1", eval( "if( ! true <= false, 1, 0)" ) );
        assertEquals( "1", eval( "if( ! true >= false, 1, 0)" ) );

        assertEquals( "0", eval( "if( ! ( true >= false ), 1, 0)" ) );
        assertEquals( "0", eval( "if( ! ( true > false ), 1, 0)" ) );
        assertEquals( "1", eval( "if( ! ( true <= false ), 1, 0)" ) );
        assertEquals( "1", eval( "if( ! ( true < false ), 1, 0)" ) );

        // Equality (associative, left/right parsing direction doesn't matter)

        assertEquals( "1", eval( "if( true == true, 1, 0)" ) );
        assertEquals( "0", eval( "if( true == false, 1, 0)" ) );

        assertEquals( "0", eval( "if( true != true, 1, 0)" ) );
        assertEquals( "1", eval( "if( true != false, 1, 0)" ) );

        assertEquals( "1", eval( "if( true == false == false, 1, 0)" ) );

        // && (and)

        assertEquals( "1", eval( "if( true && true, 1, 0)" ) );
        assertEquals( "0", eval( "if( true && false, 1, 0)" ) );
        assertEquals( "0", eval( "if( false && true, 1, 0)" ) );
        assertEquals( "0", eval( "if( false && false, 1, 0)" ) );

        // && (and) after Equality

        assertEquals( "1", eval( "if( true && 1 == 1, 1, 0)" ) );
        assertNull( error( "if( ( true && 1 ) == 1, 1, 0)" ) );

        // || (or)

        assertEquals( "1", eval( "if( true || true, 1, 0)" ) );
        assertEquals( "1", eval( "if( true || false, 1, 0)" ) );
        assertEquals( "1", eval( "if( false || true, 1, 0)" ) );
        assertEquals( "0", eval( "if( false || false, 1, 0)" ) );

        // || (or) after && (and)

        assertEquals( "1", eval( "if( true || true && false, 1, 0)" ) );
        assertEquals( "0", eval( "if( ( true || true ) && false, 1, 0)" ) );
    }

    @Test
    public void testExpressionItems()
    {
        assertEquals( "HllvX50cXC0", categoryService.getDefaultCategoryOptionCombo().getUid() );

        // Data element

        assertEquals( "3 DeA", eval( "#{dataElemenA}" ) );
        assertEquals( "13 DeB", eval( "#{dataElemenB}" ) );

        // Data element with non-numeric values

        assertEquals( "'Str' DeF", evalPredictor( "#{dataElemenF}", TEXT ) );
        assertEquals( "'2022-01-15' DeG", evalPredictor( "#{dataElemenG}", TEXT ) );
        assertEquals( "true DeH", evalPredictor( "#{dataElemenH}", BOOLEAN ) );

        assertEquals( "0 DeF", eval( "if(#{dataElemenF}=='XYZ',1,0)" ) );
        assertEquals( "1 DeF", eval( "if(#{dataElemenF}=='Str',1,0)" ) );
        assertEquals( "0 DeG", eval( "if(#{dataElemenG}<'2022-01-01',1,0)" ) );
        assertEquals( "1 DeG", eval( "if(#{dataElemenG}<'2022-02-01',1,0)" ) );
        assertEquals( "0 DeH", eval( "if(!#{dataElemenH},1,0)" ) );
        assertEquals( "1 DeH", eval( "if(#{dataElemenH},1,0)" ) );

        // Data element operand

        assertEquals( "5 DeA CocB", eval( "#{dataElemenA.catOptCombB}" ) );
        assertEquals( "15 DeB CocA", eval( "#{dataElemenB.catOptCombA}" ) );
        assertEquals( "5 DeA CocB", eval( "#{dataElemenA.catOptCombB.*}" ) );
        assertEquals( "15 DeB CocA", eval( "#{dataElemenB.catOptCombA.*}" ) );
        assertEquals( "7 DeA CocA CocB", eval( "#{dataElemenA.catOptCombA.catOptCombB}" ) );
        assertEquals( "17 DeB CocB CocA", eval( "#{dataElemenB.catOptCombB.catOptCombA}" ) );
        assertEquals( "9 DeA * CocB", eval( "#{dataElemenA.*.catOptCombB}" ) );
        assertEquals( "19 DeB * CocA", eval( "#{dataElemenB.*.catOptCombA}" ) );

        // Indicator operand

        assertEquals( "88 IndicatorA", eval( "N{mindicatorA}" ) );

        // Program data element

        assertEquals( "101 PA DeC", eval( "D{programUidA.dataElemenC}" ) );
        assertEquals( "102 PB DeD", eval( "D{programUidB.dataElemenD}" ) );

        // Program attribute (a.k.a. Program tracked entity attribute)

        assertEquals( "201 PA TeaA", eval( "A{programUidA.trakEntAttA}" ) );
        assertEquals( "202 PB TeaB", eval( "A{programUidB.trakEntAttB}" ) );

        // Program indicator

        assertEquals( "301 PiA", eval( "I{programIndA}" ) );
        assertEquals( "302 PiB", eval( "I{programIndB}" ) );

        // Data set reporting rate

        assertEquals( "401 DsA - Reporting rate", eval( "R{dataSetUidA.REPORTING_RATE}" ) );
        assertEquals( "402 DsA - Reporting rate on time", eval( "R{dataSetUidA.REPORTING_RATE_ON_TIME}" ) );
        assertEquals( "403 DsA - Actual reports", eval( "R{dataSetUidA.ACTUAL_REPORTS}" ) );
        assertEquals( "404 DsA - Actual reports on time", eval( "R{dataSetUidA.ACTUAL_REPORTS_ON_TIME}" ) );
        assertEquals( "405 DsA - Expected reports", eval( "R{dataSetUidA.EXPECTED_REPORTS}" ) );
        assertEquals( "406 DsB - Reporting rate", eval( "R{dataSetUidB.REPORTING_RATE}" ) );

        // Constant

        assertEquals( "0.5", eval( "C{xxxxxxxxx05}" ) );
        assertEquals( "0.25", eval( "C{xxxxxxxx025}" ) );

        // Org unit group

        assertEquals( "1000000", eval( "OUG{orgUnitGrpA}" ) );
        assertEquals( "2000000", eval( "OUG{orgUnitGrpB}" ) );

        // Days

        assertEquals( "30", eval( "[days]" ) );
    }

    @Test
    public void testExpressionLogical()
    {
        // If function is tested elsewhere

        // IsNull

        assertEquals( "0 DeA", eval( "if( isNull( #{dataElemenA} ), 1, 0)", NEVER_SKIP ) );
        assertEquals( "1 DeE", eval( "if( isNull( #{dataElemenE} ), 1, 0)", NEVER_SKIP ) );

        // IsNotNull

        assertEquals( "1 DeA", eval( "if( isNotNull( #{dataElemenA} ), 1, 0)", NEVER_SKIP ) );
        assertEquals( "0 DeE", eval( "if( isNotNull( #{dataElemenE} ), 1, 0)", NEVER_SKIP ) );

        // FirstNonNull

        assertEquals( "3 DeA", eval( "firstNonNull( #{dataElemenA} )", NEVER_SKIP ) );
        assertEquals( "3 DeA DeE", eval( "firstNonNull( #{dataElemenA}, #{dataElemenE} )", NEVER_SKIP ) );
        assertEquals( "3 DeA DeE", eval( "firstNonNull( #{dataElemenE}, #{dataElemenA} )", NEVER_SKIP ) );
        assertEquals( "3 DeA DeC DeE",
            eval( "firstNonNull( #{dataElemenA}, #{dataElemenC}, #{dataElemenE} )", NEVER_SKIP ) );
        assertEquals( "3 DeA DeC DeE",
            eval( "firstNonNull( #{dataElemenC}, #{dataElemenE}, #{dataElemenA} )", NEVER_SKIP ) );
        assertEquals( "3 DeA DeC DeE",
            eval( "firstNonNull( #{dataElemenE}, #{dataElemenA}, #{dataElemenC} )", NEVER_SKIP ) );

        // Greatest

        assertEquals( "5", eval( "greatest( 3, 4, 1, 5, 2 )" ) );
        assertEquals( "null DeE", eval( "greatest( #{dataElemenE} )" ) );

        // Least

        assertEquals( "1", eval( "least( 3, 4, 1, 5, 2 )" ) );
        assertEquals( "null DeE", eval( "least( #{dataElemenE} )" ) );
    }

    @Test
    public void testExpressionMissingValueStrategy()
    {
        assertEquals( "3 DeA", eval( "#{dataElemenA}", SKIP_IF_ANY_VALUE_MISSING ) );
        assertEquals( "16 DeA DeB", eval( "#{dataElemenA} + #{dataElemenB}", SKIP_IF_ANY_VALUE_MISSING ) );
        assertEquals( "null DeA DeB DeC",
            eval( "#{dataElemenA} + #{dataElemenB} + #{dataElemenC}", SKIP_IF_ANY_VALUE_MISSING ) );
        assertEquals( "null DeC DeD DeE",
            eval( "#{dataElemenC} + #{dataElemenD} + #{dataElemenE}", SKIP_IF_ANY_VALUE_MISSING ) );
        assertEquals( "null DeE", eval( "#{dataElemenE}", SKIP_IF_ANY_VALUE_MISSING ) );

        assertEquals( "3 DeA", eval( "#{dataElemenA}", SKIP_IF_ALL_VALUES_MISSING ) );
        assertEquals( "16 DeA DeB", eval( "#{dataElemenA} + #{dataElemenB}", SKIP_IF_ALL_VALUES_MISSING ) );
        assertEquals( "16 DeA DeB DeC",
            eval( "#{dataElemenA} + #{dataElemenB} + #{dataElemenC}", SKIP_IF_ALL_VALUES_MISSING ) );
        assertEquals( "null DeC DeD DeE",
            eval( "#{dataElemenC} + #{dataElemenD} + #{dataElemenE}", SKIP_IF_ALL_VALUES_MISSING ) );
        assertEquals( "null DeE", eval( "#{dataElemenE}", SKIP_IF_ALL_VALUES_MISSING ) );

        assertEquals( "3 DeA", eval( "#{dataElemenA}", NEVER_SKIP ) );
        assertEquals( "16 DeA DeB", eval( "#{dataElemenA} + #{dataElemenB}", NEVER_SKIP ) );
        assertEquals( "16 DeA DeB DeC", eval( "#{dataElemenA} + #{dataElemenB} + #{dataElemenC}", NEVER_SKIP ) );
        assertEquals( "0 DeC DeD DeE", eval( "#{dataElemenC} + #{dataElemenD} + #{dataElemenE}", NEVER_SKIP ) );
        assertEquals( "0 DeE", eval( "#{dataElemenE}", NEVER_SKIP ) );
    }

    @Test
    public void testExpressionPredictorMissingValueStrategy()
    {
        assertEquals( "null DeA", evalPredictor( "sum(#{dataElemenA} + #{dataElemenA})", SKIP_IF_ANY_VALUE_MISSING ) );
        assertEquals( "null DeA DeB",
            evalPredictor( "sum(#{dataElemenA} + #{dataElemenB})", SKIP_IF_ANY_VALUE_MISSING ) );
        assertEquals( "4 DeB DeC", evalPredictor( "sum(#{dataElemenB} + #{dataElemenC})", SKIP_IF_ANY_VALUE_MISSING ) );

        assertEquals( "null DeA", evalPredictor( "sum(#{dataElemenA} + #{dataElemenA})", SKIP_IF_ALL_VALUES_MISSING ) );
        assertEquals( "1 DeA DeB",
            evalPredictor( "sum(#{dataElemenA} + #{dataElemenB})", SKIP_IF_ALL_VALUES_MISSING ) );
        assertEquals( "6 DeB DeC",
            evalPredictor( "sum(#{dataElemenB} + #{dataElemenC})", SKIP_IF_ALL_VALUES_MISSING ) );

        assertEquals( "0 DeA", evalPredictor( "sum(#{dataElemenA} + #{dataElemenA})", NEVER_SKIP ) );
        assertEquals( "1 DeA DeB", evalPredictor( "sum(#{dataElemenA} + #{dataElemenB})", NEVER_SKIP ) );
        assertEquals( "6 DeB DeC", evalPredictor( "sum(#{dataElemenB} + #{dataElemenC})", NEVER_SKIP ) );
    }

    @Test
    public void testGetExpressionOrgUnitGroups()
    {
        assertEquals( "", getOrgUnitGroups( "#{dataElemenA} " ) );
        assertEquals( "OugA", getOrgUnitGroups( "OUG{orgUnitGrpA}" ) );
        assertEquals( "OugA, OugB, OugC",
            getOrgUnitGroups( "OUG{orgUnitGrpA} + OUG{orgUnitGrpB} + OUG{orgUnitGrpC}" ) );
        assertEquals( "OugA, OugB, OugC",
            getOrgUnitGroups( "if(orgUnit.group(orgUnitGrpA,orgUnitGrpB,orgUnitGrpC),1,0)" ) );
    }

    @Test
    public void testGetExpressionDescription()
    {
        assertEquals( "DeA", desc( "#{dataElemenA}" ) );
        assertEquals( "( DeA - DeB ) / DeC ^ DeD",
            desc( "( #{dataElemenA} - #{dataElemenB} ) / #{dataElemenC} ^ #{dataElemenD}" ) );
        assertEquals( "PA DeC*PB DeD", desc( "D{programUidA.dataElemenC}*D{programUidB.dataElemenD}" ) );
        assertEquals( "PA TeaA / PB TeaB", desc( "A{programUidA.trakEntAttA} / A{programUidB.trakEntAttB}" ) );
        assertEquals( "PiA % PiB", desc( "I{programIndA} % I{programIndB}" ) );
        assertEquals( "DsA - Reporting rate ^ DsB - Actual reports",
            desc( "R{dataSetUidA.REPORTING_RATE} ^ R{dataSetUidB.ACTUAL_REPORTS}" ) );
        assertEquals( "One half + One quarter", desc( "C{xxxxxxxxx05} + C{xxxxxxxx025}" ) );
        assertEquals( "OugA - OugB", desc( "OUG{orgUnitGrpA} - OUG{orgUnitGrpB}" ) );
        assertEquals( "1 + [Number of days]", desc( "1 + [days]" ) );
        assertEquals( "if(orgUnit.ancestor(OuA,OuB),1,0)",
            desc( "if(orgUnit.ancestor(OrgUnitUidA,OrgUnitUidB),1,0)" ) );
        assertEquals( "if(orgUnit.group(OugA,OugB),1,0)",
            desc( "if(orgUnit.group(orgUnitGrpA,orgUnitGrpB),1,0)" ) );
    }

    @Test
    public void testBadExpressions()
    {
        assertNull( error( "( 1" ) );
        assertNull( error( "( 1 +" ) );
        assertNull( error( "1) + 2" ) );
        assertNull( error( "abc" ) );
        assertNull( error( "'abc'" ) );
        assertNull( error( "if(0, 1, 0)" ) );
        assertNull( error( "1 && true" ) );
        assertNull( error( "true && 2" ) );
        assertNull( error( "!5" ) );
        assertNull( error( "true / ( #{dataElemenA} - #{dataElemenB} )" ) );
        assertNull( error( "#{dataElemenF}" ) );
        assertNull( error( "#{dataElemenG}" ) );
        assertNull( error( "#{dataElemenH}" ) );
    }

    // -------------------------------------------------------------------------
    // Indicator expression tests
    // -------------------------------------------------------------------------

    @Test
    public void testMultipleNestedIndicators()
    {
        Indicator indicatorB = createIndicator( 'B', indicatorTypeB, "10" );
        Indicator indicatorC = createIndicator( 'C', indicatorTypeB, "20" );
        Indicator indicatorD = createIndicator( 'D', indicatorTypeB, "30" );
        Indicator indicatorE = createIndicator( 'E', indicatorTypeB, "N{mindicatorC}*N{mindicatorB}-N{mindicatorD}" );

        DimensionalItemId idB = new DimensionalItemId( DimensionItemType.INDICATOR, indicatorB.getUid() );
        DimensionalItemId idC = new DimensionalItemId( DimensionItemType.INDICATOR, indicatorC.getUid() );
        DimensionalItemId idD = new DimensionalItemId( DimensionItemType.INDICATOR, indicatorD.getUid() );

        List<Indicator> indicators = singletonList( indicatorE );

        Map<DimensionalItemId, DimensionalItemObject> expectedItemMap = ImmutableMap.of(
            idB, indicatorB,
            idC, indicatorC,
            idD, indicatorD );

        Map<DimensionalItemId, DimensionalItemObject> itemMap = expressionService
            .getIndicatorDimensionalItemMap( indicators );

        assertMapEquals( expectedItemMap, itemMap );
    }

    @Test
    public void testGetIndicatorDimensionalItemMap()
    {
        Indicator indicatorA = createIndicator( 'A', indicatorTypeA );
        indicatorA.setNumerator( "#{dataElemenA.catOptCombB}*C{xxxxxxxxx05}" );
        indicatorA.setDenominator( "#{dataElemenB.catOptCombA}" );

        Indicator indicatorB = createIndicator( 'B', indicatorTypeA );
        indicatorB.setNumerator( "R{dataSetUidA.REPORTING_RATE} * A{programUidA.trakEntAttA}" );
        indicatorB.setDenominator( null );

        List<Indicator> indicators = Arrays.asList( indicatorA, indicatorB );

        DimensionalItemId id1 = new DimensionalItemId( DimensionItemType.DATA_ELEMENT_OPERAND,
            dataElementA.getUid(), categoryOptionComboB.getUid() );

        DimensionalItemId id2 = new DimensionalItemId( DimensionItemType.DATA_ELEMENT_OPERAND,
            dataElementB.getUid(), categoryOptionComboA.getUid() );

        DimensionalItemId id3 = new DimensionalItemId( DimensionItemType.REPORTING_RATE,
            dataSetA.getUid(), "REPORTING_RATE" );

        DimensionalItemId id4 = new DimensionalItemId( DimensionItemType.PROGRAM_ATTRIBUTE,
            programA.getUid(), trackedEntityAttributeA.getUid() );

        Map<DimensionalItemId, DimensionalItemObject> expectedItemMap = ImmutableMap.of(
            id1, new DataElementOperand( dataElementA, categoryOptionComboB ),
            id2, new DataElementOperand( dataElementB, categoryOptionComboA ),
            id3, new ReportingRate( dataSetA ),
            id4, new ProgramTrackedEntityAttributeDimensionItem( programA, trackedEntityAttributeA ) );

        Map<DimensionalItemId, DimensionalItemObject> itemMap = expressionService
            .getIndicatorDimensionalItemMap( indicators );

        assertMapEquals( expectedItemMap, itemMap );
    }

    @Test
    public void testGetIndicatorOrgUnitGroups()
    {
        Indicator indicatorA = createIndicator( 'A', indicatorTypeA );
        indicatorA.setNumerator( "#{dataElemenA.catOptCombB} + OUG{orgUnitGrpA} + OUG{orgUnitGrpB}" );
        indicatorA.setDenominator( "1" );

        Indicator indicatorB = createIndicator( 'B', indicatorTypeA );
        indicatorB.setNumerator( "OUG{orgUnitGrpC}" );
        indicatorB.setDenominator( null );

        List<Indicator> indicators = Arrays.asList( indicatorA, indicatorB );

        Set<OrganisationUnitGroup> items = expressionService.getIndicatorOrgUnitGroups( indicators );

        assertEquals( 3, items.size() );

        List<String> nameList = items.stream().map( BaseIdentifiableObject::getName ).sorted()
            .collect( Collectors.toList() );

        String names = String.join( ",", nameList );

        assertEquals( "OugA,OugB,OugC", names );
    }

    @Test
    public void testGetIndicatorDimensionalItemMap2()
    {
        Indicator indicatorA = createIndicator( 'A', indicatorTypeA );
        indicatorA.setNumerator( "#{dataElemenA.catOptCombB}*C{xxxxxxxxx05}" );
        indicatorA.setDenominator( "#{dataElemenA.catOptCombB}" );

        Indicator indicatorB = createIndicator( 'B', indicatorTypeA );
        indicatorB.setNumerator( "#{dataElemenA.catOptCombB} + #{dataElemenB.catOptCombA}" );
        indicatorB.setDenominator( "#{dataElemenA.catOptCombB}" );
        indicatorB.setAnnualized( true );

        Period period = createPeriod( "20010101" );

        List<Indicator> indicators = Arrays.asList( indicatorA, indicatorB );

        Map<DimensionalItemId, DimensionalItemObject> itemMap = expressionService
            .getIndicatorDimensionalItemMap( indicators );

        IndicatorValue value = expressionService.getIndicatorValueObject( indicatorA,
            singletonList( period ), itemMap, valueMap, constantMap, null );

        assertEquals( 2.5, value.getNumeratorValue(), DELTA );
        assertEquals( 5.0, value.getDenominatorValue(), DELTA );
        assertEquals( 100.0, value.getFactor(), DELTA );
        assertEquals( 100, value.getMultiplier(), DELTA );
        assertEquals( 1, value.getDivisor(), DELTA );
        assertEquals( 50.0, value.getValue(), DELTA );

        value = expressionService.getIndicatorValueObject( indicatorB,
            singletonList( period ), itemMap, valueMap, constantMap, null );

        assertEquals( 20.0, value.getNumeratorValue(), DELTA );
        assertEquals( 5.0, value.getDenominatorValue(), DELTA );
        assertEquals( 36500.0, value.getFactor(), DELTA );
        assertEquals( 36500, value.getMultiplier(), DELTA );
        assertEquals( 1, value.getDivisor(), DELTA );
        assertEquals( 146000.0, value.getValue(), DELTA );
    }

    private Indicator createIndicator( char uniqueCharacter, IndicatorType type, String numerator )
    {
        Indicator indicator = createIndicator( uniqueCharacter, type );

        indicator.setUid( "mindicator" + uniqueCharacter );
        indicator.setNumerator( numerator );
        indicator.setDenominator( "1" );

        indicatorService.addIndicator( indicator );
        return indicator;
    }

    // -------------------------------------------------------------------------
    // Valid expression tests
    // -------------------------------------------------------------------------

    @Test
    public void testIndicatorExpressionIsValid()
    {
        assertEquals( VALID,
            validity( "#{dataElemenA.catOptCombB}*C{xxxxxxxxx05}", INDICATOR_EXPRESSION ) );

        assertEquals( EXPRESSION_IS_NOT_WELL_FORMED,
            validity( "stddev(#{dataElemenA.catOptCombB}*C{xxxxxxxxx05})", INDICATOR_EXPRESSION ) );

        assertEquals( VALID,
            validity( "greatest(#{dataElemenA.catOptCombB},C{xxxxxxxxx05})", INDICATOR_EXPRESSION ) );

        assertEquals( EXPRESSION_IS_NOT_WELL_FORMED,
            validity( "1*", INDICATOR_EXPRESSION ) );
    }

    @Test
    public void testValidationRuleExpressionIsValid()
    {
        assertEquals( VALID,
            validity( "#{dataElemenA.catOptCombB}*C{xxxxxxxxx05}", VALIDATION_RULE_EXPRESSION ) );

        assertEquals( EXPRESSION_IS_NOT_WELL_FORMED,
            validity( "stddev(#{dataElemenA.catOptCombB}*C{xxxxxxxxx05})", VALIDATION_RULE_EXPRESSION ) );

        assertEquals( VALID,
            validity( "greatest(#{dataElemenA.catOptCombB},C{xxxxxxxxx05})", VALIDATION_RULE_EXPRESSION ) );

        assertEquals( EXPRESSION_IS_NOT_WELL_FORMED,
            validity( "1*", VALIDATION_RULE_EXPRESSION ) );
    }

    @Test
    public void testPredictorExpressionIsValid()
    {
        assertEquals( VALID,
            validity( "#{dataElemenA.catOptCombB}*C{xxxxxxxxx05}", PREDICTOR_EXPRESSION ) );

        assertEquals( VALID,
            validity( "stddev(#{dataElemenA.catOptCombB}*C{xxxxxxxxx05})", PREDICTOR_EXPRESSION ) );

        assertEquals( VALID,
            validity( "greatest(#{dataElemenA.catOptCombB},C{xxxxxxxxx05})", PREDICTOR_EXPRESSION ) );

        assertEquals( EXPRESSION_IS_NOT_WELL_FORMED,
            validity( "1*", PREDICTOR_EXPRESSION ) );
    }
}
