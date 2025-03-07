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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.webapi.WebClient.Body;
import static org.hisp.dhis.webapi.WebClient.ContentType;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.metadata.MetadataProposalStatus;
import org.hisp.dhis.metadata.MetadataProposalTarget;
import org.hisp.dhis.metadata.MetadataProposalType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.controller.json.JsonMetadataProposal;
import org.hisp.dhis.webapi.controller.metadata.MetadataWorkflowController;
import org.hisp.dhis.webapi.json.JsonObject;
import org.hisp.dhis.webapi.json.domain.JsonErrorReport;
import org.hisp.dhis.webapi.json.domain.JsonOrganisationUnit;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * Tests the {@link MetadataWorkflowController}.
 *
 * @author Jan Bernitt
 */
public class MetadataWorkflowControllerTest extends DhisControllerConvenienceTest
{
    private String defaultTargetUid;

    private User system;

    @Before
    public void setUp()
    {
        defaultTargetUid = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits/", "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}" ) );

        // make sure a system user exist that can add/update/delete OUs
        system = switchToNewUser( "system", "F_ORGANISATIONUNIT_ADD", "F_ORGANISATIONUNIT_DELETE" );
        // and back to being SU for further setup in the test scenarios
        switchToSuperuser();
    }

    @Test
    public void testGetProposals()
    {
        String proposalId = postAddProposal( "My Unit", "OU1" );
        assertNotNull( proposalId );

        JsonObject page = GET( "/metadata/proposals/" ).content();
        assertTrue( page.has( "pager", "proposals" ) );
        assertEquals( 1, page.getArray( "proposals" ).size() );
        assertEquals( proposalId, page.getArray( "proposals" ).getObject( 0 ).getString( "id" ).string() );
    }

    @Test
    public void testGetProposal()
    {
        String proposalId = postAddProposal( "My Unit", "OU2" );

        JsonMetadataProposal proposal = GET( "/metadata/proposals/{uid}", proposalId )
            .content().asObject( JsonMetadataProposal.class );
        assertTrue( proposal.exists() );
    }

    @Test
    public void testMakeAddProposal()
    {
        String proposalId = postAddProposal( "My Unit", "OU1" );

        JsonMetadataProposal proposal = GET( "/metadata/proposals/{uid}", proposalId )
            .content().asObject( JsonMetadataProposal.class );
        assertEquals( MetadataProposalStatus.PROPOSED, proposal.getStatus() );
        assertEquals( MetadataProposalType.ADD, proposal.getType() );
        assertEquals( MetadataProposalTarget.ORGANISATION_UNIT, proposal.getTarget() );
        assertNull( proposal.getTargetUid() );
        assertNotNull( proposal.getCreated() );
        assertNotNull( proposal.getCreatedBy() );
        assertNull( proposal.getFinalisedBy() );
        assertNull( proposal.getFinalised() );
        assertEquals( "We need it", proposal.getComment() );
        assertNull( proposal.getReason() );
        assertTrue( proposal.getChange().isObject() );
    }

    @Test
    public void testMakeAddProposal_BadRequestNoChange()
    {
        assertWebMessage( "Bad Request", 400, "ERROR",
            "`change` is required for type ADD",
            POST( "/metadata/proposals/", "{"
                + "'type':'ADD',"
                + "'target':'ORGANISATION_UNIT',"
                + "'change':null}" )
                    .content( HttpStatus.BAD_REQUEST ) );
    }

    @Test
    public void testMakeAddProposal_ConflictIllegalChange()
    {
        JsonWebMessage message = assertWebMessage( "Conflict", 409, "WARNING",
            "One more more errors occurred, please see full details in import report.",
            POST( "/metadata/proposals/", "{"
                + "'type':'ADD'," + "'target':'ORGANISATION_UNIT',"
                + "'change':{'name':'hasNoShortName', "
                + "'openingDate': '2020-01-01'" + "}" + "}" )
                    .content( HttpStatus.CONFLICT ) );
        JsonErrorReport error = message.find( JsonErrorReport.class,
            report -> report.getErrorCode() == ErrorCode.E4000 );
        assertEquals( "Missing required property `shortName`.", error.getMessage() );
        assertEquals( "shortName", error.getErrorProperties().get( 0 ) );
    }

    @Test
    public void testMakeUpdateProposal()
    {
        String proposalId = postUpdateNameProposal( defaultTargetUid, "New Name" );
        JsonMetadataProposal proposal = GET( "/metadata/proposals/{uid}", proposalId )
            .content().asObject( JsonMetadataProposal.class );
        assertEquals( MetadataProposalStatus.PROPOSED, proposal.getStatus() );
        assertEquals( MetadataProposalType.UPDATE, proposal.getType() );
        assertEquals( MetadataProposalTarget.ORGANISATION_UNIT, proposal.getTarget() );
        assertEquals( defaultTargetUid, proposal.getTargetUid() );
        assertNotNull( proposal.getCreated() );
        assertNotNull( proposal.getCreatedBy() );
        assertNull( proposal.getFinalisedBy() );
        assertNull( proposal.getFinalised() );
        assertNull( proposal.getComment() );
        assertNull( proposal.getReason() );
        assertTrue( proposal.getChange().isArray() );
    }

    @Test
    public void testMakeUpdateProposal_BadRequestMissingTargetUid()
    {
        assertWebMessage( "Bad Request", 400, "ERROR",
            "`targetUid` is required for type UPDATE",
            POST( "/metadata/proposals/", "{" +
                "'type':'UPDATE'," +
                "'target':'ORGANISATION_UNIT'," +
                "'change':[{'op':'replace', 'path':'/name', 'value':'New name'}]" +
                "}" ).content( HttpStatus.BAD_REQUEST ) );
    }

    @Test
    public void testMakeUpdateProposal_BadRequestChangeObject()
    {
        assertWebMessage( "Bad Request", 400, "ERROR",
            "`change` must be a non empty array for type UPDATE",
            POST( "/metadata/proposals/", "{" +
                "'type':'UPDATE'," +
                "'target':'ORGANISATION_UNIT'," +
                "'targetUid': '" + defaultTargetUid + "'," +
                "'change':{}" +
                "}" ).content( HttpStatus.BAD_REQUEST ) );
    }

    @Test
    public void testMakeUpdateProposal_ConflictIllegalChange()
    {
        JsonWebMessage message = assertWebMessage( "Conflict", 409, "WARNING",
            "One more more errors occurred, please see full details in import report.",
            POST( "/metadata/proposals/", "{" +
                "'type':'UPDATE'," +
                "'target':'ORGANISATION_UNIT'," +
                "'targetUid': '" + defaultTargetUid + "'," +
                "'change':[{'op':'not-json-patch-op'}]" +
                "}" ).content( HttpStatus.CONFLICT ) );
        JsonErrorReport error = message.find( JsonErrorReport.class,
            report -> report.getErrorCode() == ErrorCode.E4031 );
        assertEquals(
            "Property `change` requires a valid JSON payload, was given `[{\"op\":\"not-json-patch-op\"}]`.",
            error.getMessage() );
        assertEquals( "change", error.getErrorProperties().get( 0 ) );
    }

    @Test
    public void testMakeRemoveProposal()
    {
        String proposalId = postRemoveProposal( defaultTargetUid );
        JsonMetadataProposal proposal = GET( "/metadata/proposals/{uid}", proposalId )
            .content().asObject( JsonMetadataProposal.class );
        assertEquals( MetadataProposalStatus.PROPOSED, proposal.getStatus() );
        assertEquals( MetadataProposalType.REMOVE, proposal.getType() );
        assertEquals( MetadataProposalTarget.ORGANISATION_UNIT, proposal.getTarget() );
        assertEquals( defaultTargetUid, proposal.getTargetUid() );
        assertNotNull( proposal.getCreated() );
        assertNotNull( proposal.getCreatedBy() );
        assertNull( proposal.getFinalisedBy() );
        assertNull( proposal.getFinalised() );
        assertNull( proposal.getComment() );
        assertNull( proposal.getReason() );
        assertTrue( proposal.getChange().isUndefined() );
    }

    @Test
    public void testMakeRemoveProposal_BadRequestMissingTargetUid()
    {
        assertWebMessage( "Bad Request", 400, "ERROR",
            "`targetUid` is required for type REMOVE",
            POST( "/metadata/proposals/", "{" +
                "'type':'REMOVE'," +
                "'target':'ORGANISATION_UNIT'" +
                "}" ).content( HttpStatus.BAD_REQUEST ) );
    }

    @Test
    public void testAcceptAddProposal()
    {
        String proposalId = postAddProposal( "My OU", "OU1" );

        String ouId = assertStatus( HttpStatus.CREATED, POST( "/metadata/proposals/" + proposalId ) );

        JsonOrganisationUnit ou = GET( "/organisationUnits/{uid}", ouId )
            .content().as( JsonOrganisationUnit.class );
        assertEquals( "My OU", ou.getName() );

        JsonMetadataProposal proposal = GET( "/metadata/proposals/{uid}", proposalId )
            .content().asObject( JsonMetadataProposal.class );
        assertEquals( MetadataProposalStatus.ACCEPTED, proposal.getStatus() );
        assertNotNull( proposal.getFinalisedBy() );
        assertNotNull( proposal.getFinalised() );
    }

    @Test
    public void testAcceptUpdateProposal()
    {
        String proposalId = postUpdateNameProposal( defaultTargetUid, "New name" );

        assertStatus( HttpStatus.OK, POST( "/metadata/proposals/" + proposalId ) );

        JsonOrganisationUnit ou = GET( "/organisationUnits/{uid}", defaultTargetUid )
            .content().as( JsonOrganisationUnit.class );
        assertEquals( "New name", ou.getName() );

        JsonMetadataProposal proposal = GET( "/metadata/proposals/{uid}", proposalId )
            .content().asObject( JsonMetadataProposal.class );
        assertEquals( MetadataProposalStatus.ACCEPTED, proposal.getStatus() );
        assertNotNull( proposal.getFinalisedBy() );
        assertNotNull( proposal.getFinalised() );
    }

    @Test
    public void testAcceptUpdateProposal_ConflictTargetAlreadyDeleted()
    {
        String proposalId = postUpdateNameProposal( defaultTargetUid, "New name" );

        assertStatus( HttpStatus.OK, DELETE( "/organisationUnits/" + defaultTargetUid ) );

        JsonWebMessage message = assertWebMessage( "Conflict", 409, "WARNING",
            "One more more errors occurred, please see full details in import report.",
            POST( "/metadata/proposals/" + proposalId ).content( HttpStatus.CONFLICT ) );
        JsonErrorReport error = message.find( JsonErrorReport.class,
            report -> report.getErrorCode() == ErrorCode.E4015 );
        assertEquals(
            "Property `targetUid` refers to an object that does not exist, could not find `" + defaultTargetUid + "`",
            error.getMessage() );
        assertEquals( "targetUid", error.getErrorProperties().get( 0 ) );
    }

    @Test
    public void testAcceptRemoveProposal()
    {
        String proposalId = postRemoveProposal( defaultTargetUid );

        assertStatus( HttpStatus.OK, POST( "/metadata/proposals/" + proposalId ) );

        assertStatus( HttpStatus.NOT_FOUND, GET( "/organisationUnits/{uid}", defaultTargetUid ) );

        JsonMetadataProposal proposal = GET( "/metadata/proposals/{uid}", proposalId )
            .content().asObject( JsonMetadataProposal.class );
        assertEquals( MetadataProposalStatus.ACCEPTED, proposal.getStatus() );
        assertNotNull( proposal.getFinalisedBy() );
        assertNotNull( proposal.getFinalised() );
    }

    @Test
    public void testAcceptRemoveProposal_ConflictTargetAlreadyDeleted()
    {
        String proposalId = postRemoveProposal( defaultTargetUid );

        assertStatus( HttpStatus.OK, DELETE( "/organisationUnits/" + defaultTargetUid ) );

        JsonWebMessage message = assertWebMessage( "Conflict", 409, "WARNING",
            "One more more errors occurred, please see full details in import report.",
            POST( "/metadata/proposals/" + proposalId ).content( HttpStatus.CONFLICT ) );
        JsonErrorReport error = message.find( JsonErrorReport.class,
            report -> report.getErrorCode() == ErrorCode.E4015 );
        assertEquals(
            "Property `targetUid` refers to an object that does not exist, could not find `" + defaultTargetUid + "`",
            error.getMessage() );
        assertEquals( "targetUid", error.getErrorProperties().get( 0 ) );
    }

    @Test
    public void testAcceptProposal_ConflictAlreadyRejected()
    {
        String proposalId = postRemoveProposal( defaultTargetUid );

        // reject
        assertStatus( HttpStatus.NO_CONTENT, DELETE( "/metadata/proposals/" + proposalId ) );

        assertStatus( HttpStatus.CONFLICT, POST( "/metadata/proposals/" + proposalId ) );
    }

    @Test
    public void testAcceptProposal_ConflictTargetAlreadyDeleted()
    {
        String proposalId = postRemoveProposal( defaultTargetUid );

        assertStatus( HttpStatus.OK, DELETE( "/organisationUnits/" + defaultTargetUid ) );
        assertStatus( HttpStatus.CONFLICT, POST( "/metadata/proposals/" + proposalId ) );

        JsonMetadataProposal proposal = GET( "/metadata/proposals/{uid}", proposalId ).content()
            .asObject( JsonMetadataProposal.class );
        assertEquals( MetadataProposalStatus.NEEDS_UPDATE, proposal.getStatus() );
        assertNotNull( proposal.getReason() );

        assertWebMessage( "Conflict", 409, "ERROR", "Proposal must be in status PROPOSED for this action",
            POST( "/metadata/proposals/" + proposalId ).content( HttpStatus.CONFLICT ) );

    }

    @Test
    public void testAcceptProposal_ConflictMissingAuthority()
    {
        User guest = switchToNewUser( "guest" );
        String proposalId = postUpdateNameProposal( defaultTargetUid, "New name" );

        JsonWebMessage message = assertWebMessage( "Conflict", 409, "WARNING",
            "One more more errors occurred, please see full details in import report.",
            POST( "/metadata/proposals/" + proposalId ).content( HttpStatus.CONFLICT ) );
        JsonErrorReport error = message.find( JsonErrorReport.class,
            report -> report.getErrorCode() == ErrorCode.E3001 );

        JsonMetadataProposal proposal = GET( "/metadata/proposals/" + proposalId )
            .content().as( JsonMetadataProposal.class );
        assertEquals( String.format(
            "E3001 User `guest guest [%s] (User)` is not allowed to update object `New name [%s] (OrganisationUnit)`.\n",
            guest.getUid(), defaultTargetUid ), proposal.getReason() );

        // but the system could accept the proposal
        switchContextToUser( system );
        assertStatus( HttpStatus.OK, PUT( "/metadata/proposals/" + proposalId ) );
        assertStatus( HttpStatus.OK, POST( "/metadata/proposals/" + proposalId ) );
    }

    @Test
    public void testAcceptProposal_NotFound()
    {
        assertStatus( HttpStatus.NOT_FOUND, POST( "/metadata/proposals/xyz" ) );
    }

    @Test
    public void testRejectProposal()
    {
        String proposalId = postRemoveProposal( defaultTargetUid );

        // reject
        assertStatus( HttpStatus.NO_CONTENT, DELETE( "/metadata/proposals/" + proposalId ) );

        JsonMetadataProposal proposal = GET( "/metadata/proposals/{uid}", proposalId ).content()
            .asObject( JsonMetadataProposal.class );
        assertEquals( MetadataProposalStatus.REJECTED, proposal.getStatus() );
        assertNotNull( proposal.getFinalisedBy() );
        assertNotNull( proposal.getFinalised() );
    }

    @Test
    public void testRejectProposal_ConflictAlreadyRejected()
    {
        String proposalId = postRemoveProposal( defaultTargetUid );

        // reject
        assertStatus( HttpStatus.NO_CONTENT, DELETE( "/metadata/proposals/" + proposalId ) );

        assertWebMessage( "Conflict", 409, "ERROR", "Proposal must be in status PROPOSED for this action",
            DELETE( "/metadata/proposals/" + proposalId ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testRejectProposal_ConflictAlreadyAccepted()
    {
        String proposalId = postRemoveProposal( defaultTargetUid );

        // accept
        assertStatus( HttpStatus.OK, POST( "/metadata/proposals/" + proposalId ) );

        assertWebMessage( "Conflict", 409, "ERROR", "Proposal must be in status PROPOSED for this action",
            DELETE( "/metadata/proposals/" + proposalId ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testRejectProposal_ConflictTargetAlreadyDeleted()
    {
        String proposalId = postRemoveProposal( defaultTargetUid );

        assertStatus( HttpStatus.OK, DELETE( "/organisationUnits/" + defaultTargetUid ) );
        assertStatus( HttpStatus.CONFLICT, POST( "/metadata/proposals/" + proposalId ) );

        assertEquals( MetadataProposalStatus.NEEDS_UPDATE, GET( "/metadata/proposals/{uid}", proposalId )
            .content().asObject( JsonMetadataProposal.class ).getStatus() );

        assertWebMessage( "Conflict", 409, "ERROR", "Proposal must be in status PROPOSED for this action",
            DELETE( "/metadata/proposals/" + proposalId ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testRejectProposal_NotFound()
    {
        assertStatus( HttpStatus.NOT_FOUND, DELETE( "/metadata/proposals/xyz" ) );
    }

    @Test
    public void testOpposeProposal()
    {
        String proposalId = postRemoveProposal( defaultTargetUid );

        assertStatus( HttpStatus.NO_CONTENT,
            PATCH( "/metadata/proposals/" + proposalId, Body( "Just NO!" ), ContentType( MediaType.TEXT_PLAIN ) ) );

        JsonMetadataProposal proposal = GET( "/metadata/proposals/{uid}", proposalId )
            .content().asObject( JsonMetadataProposal.class );
        assertEquals( "Just NO!", proposal.getReason() );
        assertEquals( MetadataProposalStatus.NEEDS_UPDATE, proposal.getStatus() );
    }

    @Test
    public void testOpposeProposal_ConflictAlreadyAccepted()
    {
        String proposalId = postRemoveProposal( defaultTargetUid );

        // accept
        assertStatus( HttpStatus.OK, POST( "/metadata/proposals/" + proposalId ) );

        assertWebMessage( "Conflict", 409, "ERROR", "Proposal must be in status PROPOSED for this action",
            PATCH( "/metadata/proposals/" + proposalId ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testOpposeProposal_NotFound()
    {
        assertStatus( HttpStatus.NOT_FOUND,
            PATCH( "/metadata/proposals/xyz", Body( "Nah" ), ContentType( MediaType.TEXT_PLAIN ) ) );
    }

    @Test
    public void testAdjustProposal()
    {
        String proposalId = postRemoveProposal( defaultTargetUid );

        assertStatus( HttpStatus.OK, DELETE( "/organisationUnits/" + defaultTargetUid ) );
        assertStatus( HttpStatus.CONFLICT, POST( "/metadata/proposals/" + proposalId ) );

        JsonMetadataProposal proposal = GET( "/metadata/proposals/{uid}", proposalId )
            .content().asObject( JsonMetadataProposal.class );
        assertNotNull( proposal.getReason() );
        assertEquals( MetadataProposalStatus.NEEDS_UPDATE, proposal.getStatus() );

        String ouId = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits/", "{'name':'My New Unit', 'shortName':'OU2', 'openingDate': '2020-01-01'}" ) );

        assertStatus( HttpStatus.OK,
            PUT( "/metadata/proposals/" + proposalId, "{'targetUid':'" + ouId + "'}" ) );

        proposal = GET( "/metadata/proposals/{uid}", proposalId )
            .content().asObject( JsonMetadataProposal.class );
        assertEquals( MetadataProposalStatus.PROPOSED, proposal.getStatus() );
        assertEquals( ouId, proposal.getTargetUid() );
    }

    private String postAddProposal( String name, String shortName )
    {
        return assertStatus( HttpStatus.CREATED,
            POST( "/metadata/proposals/", "{" +
                "'type':'ADD'," +
                "'target':'ORGANISATION_UNIT'," +
                "'change':{'name':'" + name + "', " +
                "'shortName':'" + shortName + "', " +
                "'openingDate': '2020-01-01'" +
                "}," +
                "'comment': 'We need it'" +
                "}" ) );
    }

    private String postUpdateNameProposal( String targetUid, String newName )
    {
        return assertStatus( HttpStatus.CREATED,
            POST( "/metadata/proposals/", "{" +
                "'type':'UPDATE'," +
                "'target':'ORGANISATION_UNIT'," +
                "'targetUid': '" + targetUid + "'," +
                "'change':[{'op':'replace', 'path':'/name', 'value':'" + newName + "'}]" +
                "}" ) );
    }

    private String postRemoveProposal( String targetUid )
    {
        return assertStatus( HttpStatus.CREATED,
            POST( "/metadata/proposals/", "{" +
                "'type':'REMOVE'," +
                "'target':'ORGANISATION_UNIT'," +
                "'targetUid': '" + targetUid + "'" +
                "}" ) );
    }
}
