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

package org.hisp.dhis.tracker.importer;

import com.google.gson.JsonObject;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerNtiApiTest;
import org.hisp.dhis.tracker.importer.databuilder.EnrollmentDataBuilder;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class OwnershipTests
    extends TrackerNtiApiTest
{
    String userPassword = Constants.USER_PASSWORD;

    String captureOu = "DiszpKrYNg8";

    String searchOu = "g8upMTyEZGZ";

    String trackedEntityType = Constants.TRACKED_ENTITY_TYPE;

    String username;

    String teiInSearchScope;

    String teiInCaptureScope;

    private ProgramActions programActions;

    private UserActions userActions;

    private JsonObject enrollment;

    private String protectedProgram;

    private String openProgram;

    @BeforeAll
    public void beforeAll()
        throws Exception
    {
        userActions = new UserActions();
        programActions = new ProgramActions();
        loginActions.loginAsSuperUser();
        username = createUserWithAccessToOu();
        protectedProgram = createProgramWithAccessLevel( "PROTECTED" );
        openProgram = createProgramWithAccessLevel( "OPEN" );

        String protectedProgramStageId = programActions
            .get( protectedProgram, new QueryParamsBuilder().add( "fields=programStages" ) ).validateStatus( 200 )
            .extractString( "programStages.id[0]" );

        teiInCaptureScope = super.importTeiWithEnrollmentAndEvent( captureOu, protectedProgram, protectedProgramStageId )
            .extractImportedTeis().get( 0 );
        teiInSearchScope = super.importTeiWithEnrollmentAndEvent( searchOu, protectedProgram, protectedProgramStageId )
            .extractImportedTeis().get( 0 );

        enrollment = trackerActions.getTrackedEntity( teiInSearchScope + "?fields=enrollments" )
            .validateStatus( 200 )
            .getBody();

        trackerActions.update( String
            .format( "/ownership/transfer?trackedEntityInstance=%s&program=%s&ou=%s", teiInCaptureScope, protectedProgram,
                searchOu ), new JsonObject() ).validateStatus( 200 );

    }

    @BeforeEach
    public void beforeEach()
    {
        loginActions.loginAsAdmin();
    }

    @Test
    public void shouldNotValidateCaptureScopeForTei()
    {
        JsonObject object = trackerActions.getTrackedEntity( teiInSearchScope )
            .validateStatus( 200 )
            .getBodyAsJsonBuilder()
            .wrapIntoArray( "trackedEntities" );

        loginActions.loginAsUser( username, userPassword );

        trackerActions.postAndGetJobReport( object )
            .validateSuccessfulImport();
    }

    @ValueSource( strings = { "CREATE_AND_UPDATE", "UPDATE", "DELETE" } )
    @ParameterizedTest
    public void shouldValidateEnrollmentOwnership( String importStrategy )
    {
        enrollment = trackerActions.getTrackedEntity( teiInCaptureScope + "?fields=enrollments" )
            .validateStatus( 200 )
            .getBody();

        loginActions.loginAsUser( username, userPassword );

        trackerActions.postAndGetJobReport( enrollment, new QueryParamsBuilder().add( "importStrategy=" + importStrategy ) )
            .validateErrorReport()
            .body( "errorCode", hasItems( "E1102" ) );
    }

    @Test
    public void shouldNotEnrollWhenOwnershipOutsideCaptureOu()
    {
        loginActions.loginAsUser( username, userPassword );

        trackerActions.getTrackedEntity( teiInSearchScope + "?fields=enrollments" ).validate().statusCode( 200 )
            .body( "enrollments", hasSize( 0 ) );

        trackerActions
            .postAndGetJobReport( new EnrollmentDataBuilder().build( protectedProgram, captureOu, teiInCaptureScope, "ACTIVE" ) )
            .validateErrorReport()
            .body( "errorCode", hasItems( "E1102" ) );
    }

    protected String createUserWithAccessToOu()
    {
        String username = DataGenerator.randomEntityName() + "user";
        String userid = userActions.addUser( username, Constants.USER_PASSWORD );

        //userActions.addRoleToUser( userid, auth );
        userActions.grantUserAccessToOrgUnit( userid, captureOu );
        userActions.grantUserSearchAccessToOrgUnit( userid, searchOu );
        userActions.addUserToUserGroup( userid, Constants.USER_GROUP_ID );

        return username;
    }

    @Test
    public void shouldNotValidateOwnershipIfProgramIsOpen()
    {
        loginActions.loginAsUser( username, userPassword );

        trackerActions.getTrackedEntity( teiInSearchScope + "?fields=enrollments" ).validate().statusCode( 200 )
            .body( "enrollments", hasSize( 0 ) );

        trackerActions
            .postAndGetJobReport( new EnrollmentDataBuilder().build( openProgram, captureOu, teiInCaptureScope, "ACTIVE" ) )
            .validateSuccessfulImport();

    }

    private String createProgramWithAccessLevel( String accessLevel )
    {
        String programId = programActions.createTrackerProgram( trackedEntityType, captureOu, searchOu ).extractUid();

        JsonObject program = programActions.get( programId )
            .getBodyAsJsonBuilder()
            .addProperty( "accessLevel", accessLevel )
            .addProperty( "publicAccess", "rwrw----" )
            .addProperty( "onlyEnrollOnce", "false" )
            .build();

        programActions.update( programId, program ).validateStatus( 200 );

        String programStageID = programActions.createProgramStage( "Program stage " + DataGenerator.randomString() );

        programActions.addProgramStage( programId, programStageID );

        return programId;
    }

}
