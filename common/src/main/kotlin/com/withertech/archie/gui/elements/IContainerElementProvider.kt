package com.withertech.archie.gui.elements

import java.util.function.Supplier

interface IContainerElementProvider
{
	val containerElements: List<Supplier<out IContainerElement>>
}