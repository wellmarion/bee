/*
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is mozilla.org code.
 *
 * The Initial Developer of the Original Code is
 * Bluesoft Consultoria em Informatica Ltda.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 */
package br.com.bluesoft.bee.service

import br.com.bluesoft.bee.database.ConnectionInfo
import br.com.bluesoft.bee.database.reader.DatabaseReader
import br.com.bluesoft.bee.database.reader.OracleDatabaseReader
import br.com.bluesoft.bee.importer.Importer
import br.com.bluesoft.bee.importer.JsonImporter
import br.com.bluesoft.bee.model.Schema
import br.com.bluesoft.bee.model.message.MessageLevel

class BeeSchemaValidator {

	DatabaseReader databaseReader
	Importer importer

	BeeWriter out
	String objectName
	String path
	String configName
	String clientName

	def sql


	BeeSchemaValidator(String objectName) {
		this.objectName = objectName
	}

	BeeSchemaValidator() {
		this(null)
	}

	boolean run() {
		MessagePrinter messagePrinter = new MessagePrinter()

		def importer = getImporter()
		out.log("connectiong to " + configName);
		this.databaseReader = new OracleDatabaseReader(getDatabaseConnection())

		out.log('importing schema metadata from the reference files')
		Schema metadataSchema = importer.importMetaData()

		if(objectName)
			metadataSchema = metadataSchema.filter(objectName)

		out.log('importing schema metadata from the database')
		Schema databaseSchema = databaseReader.getSchema(objectName)

		if(objectName)
			databaseSchema = databaseSchema.filter(objectName)

		out.log('validating')
		def messages = databaseSchema.validateWithMetadata(metadataSchema)
		def warnings = messages.findAll { it.level == MessageLevel.WARNING }
		def errors = messages.findAll { it.level == MessageLevel.ERROR }

		out.log("--- bee found ${warnings.size()} warnings" )
		messagePrinter.print(out, warnings)

		out.log("--- bee found ${errors.size()} errors" )
		messagePrinter.print(out, errors)

		return errors.size() == 0
	}

	private def getImporter() {
		if(importer == null)
			return new JsonImporter(path)
		return importer
	}


	def getDatabaseConnection() {
		if(sql != null) {
			return sql
		}
		return ConnectionInfo.createDatabaseConnection(configName, clientName)
	}
}
