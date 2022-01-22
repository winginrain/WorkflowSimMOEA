/* Copyright 2009-2020 David Hadka
 *
 * This file is part of the MOEA Framework.
 *
 * The MOEA Framework is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * The MOEA Framework is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the MOEA Framework.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.moeaframework.algorithm;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.moeaframework.CIRunner;
import org.moeaframework.Flaky;
import org.moeaframework.Retryable;

/**
 * Tests the {@link IBEA} class.
 */
@RunWith(CIRunner.class)
@Retryable
public class IBEATest extends AlgorithmTest {
	
	@Test
	public void testDTLZ1() throws IOException {
		test("DTLZ1_2", "IBEA", "IBEA-JMetal");
	}
	
	@Test
	public void testDTLZ2() throws IOException {
		test("DTLZ2_2", "IBEA", "IBEA-JMetal");
	}
	
	@Test
	@Flaky("need to investigate - flaky after upgrading to JMetal 5.9")
	public void testDTLZ7() throws IOException {
		test("DTLZ7_2", "IBEA", "IBEA-JMetal");
	}
	
	@Test
	public void testUF1() throws IOException {
		test("UF1", "IBEA", "IBEA-JMetal");
	}

}
