package jeb

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.shouldBeTrue

class ListCompareSpek : Spek() {init {

    given("Empty lists") {
            val lst1 = emptyList<Int>()
            val lst2 = emptyList<Int>()

            on("comparing them") {
                it("should be equal") {
                    shouldBeTrue(lst1 <= lst2 && lst1 >= lst2)
                }}}


    given("Empty list and list with 0 as last element") {
            val emptyList = emptyList<Int>()
            val zeroList = listOf(0)

            on("comparing them") {
                it("empty list should be greater than zero-list") {
                    shouldBeTrue(emptyList > zeroList)
                }
                it("zero-list should be less than empty list") {
                    shouldBeTrue(zeroList < emptyList)
                }}}


    given("List with 1 as last element and list with 0 as last element") {
            val zeroList = listOf(0)
            val oneList = listOf(1)

            on("comparing them") {
                it("zero-list should be less than one-list") {
                    shouldBeTrue(zeroList < oneList)
                }
                it("one-list should be greater than zero-list") {
                    shouldBeTrue(oneList > zeroList)
                }}}}}