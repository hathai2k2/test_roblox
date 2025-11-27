//package com.v1.example.test_ronlox
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Matrix
//import android.graphics.Paint
//import android.graphics.PointF
//import android.util.AttributeSet
//import android.view.MotionEvent
//import android.widget.FrameLayout
//import androidx.appcompat.content.res.AppCompatResources
//import androidx.core.graphics.drawable.toBitmap
//
//import kotlin.math.abs
//import kotlin.math.atan2
//import kotlin.math.sqrt
//
//class LogoView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
//
//    private var focusedComponent: Component? = null
//    private var nextFocusComponent: Component? = null
//    private var nextFocusPosition: Int = -1
//    private val deleteComponent =
//        IconFunctionComponent(AppCompatResources.getDrawable(context, R.drawable.ic_close1))
//    private val duplicateComponent =
//        IconFunctionComponent(AppCompatResources.getDrawable(context, R.drawable.ic_duplicate))
//
//    private val undoList = mutableListOf<List<Component>>()
//    private val redoList = mutableListOf<List<Component>>()
//    private var listener: LogoListener? = null
//
//    private val listComponent = mutableListOf<Component>()
//    private var downX = 0f
//    private var downY = 0f
//    private var oldDistance = 0f
//    private var oldRotation = 0f
//    private var currentType = ActionType.NONE
//    private var currentCenterPoint = PointF()
//    private var needSaveToStack = false
//    private var keepStackItem = 0
//    private val borderPoint = FloatArray(8)
//    private val borderPaint = Paint().apply {
//        style = Paint.Style.STROKE
//        color = context.getColor(R.color.color_1F1D2B)
//        strokeWidth = 0.5f.toDp()
//    }
//
//
//    private var enableColorMode = false
//    private var bitmapCopy: Bitmap? = null
//
//    override fun dispatchDraw(canvas: Canvas) {
//        drawComponent(canvas)
//        drawComponentBorder(canvas)
//        super.dispatchDraw(canvas)
//    }
//
//    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
//        return false
//    }
//
//    @SuppressLint("ClickableViewAccessibility")
//    override fun onTouchEvent(event: MotionEvent?): Boolean {
//        if (enableColorMode) {
//            handleGetColor(event)
//            return true
//        }
//        if (focusedComponent != null && focusedComponent!!.onTouchEvent(event, this)) {
//            return true
//        }
//        return when (event?.actionMasked) {
//            MotionEvent.ACTION_DOWN -> onActionDown(event)
//            MotionEvent.ACTION_POINTER_DOWN -> onActionPointerDown(event)
//            MotionEvent.ACTION_MOVE -> onActionMove(event)
//            MotionEvent.ACTION_UP -> onActionUp(event)
//            else -> super.onTouchEvent(event)
//        }
//    }
//
//    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
//        super.onSizeChanged(w, h, oldw, oldh)
//        val component = getBackgroundComponent()
//        if (component == null) {
//            val drawable = AppCompatResources.getDrawable(context, R.drawable.bg_empty)
//            drawable?.let {
//                val bitmap = drawable.toBitmap()
//                val newComponent = BackGroundComponent(bitmap)
//                newComponent.setBound(w.toFloat(), h.toFloat())
//                keepStackItem = 1
//                listComponent.add(newComponent)
//                saveStack()
//            }
//        } else {
//            component.setBound(w.toFloat(), h.toFloat())
//            if (undoList.isNotEmpty()) {
//                undoList[0] = listOf(component.copy(context))
//            }
//        }
//    }
//
//    fun scale(factor: Float, component: Component? = null) {
//        (component ?: focusedComponent)?.let {
//            val oldFactor = it.getScaleFactor()
//            val newFactor = oldFactor * factor
//            if (newFactor > it.getMaxScaleFactor() || newFactor < it.getMinScaleFactor() || factor.isNaN() || factor == 0f) {
//                return
//            }
//            it.getMappedCenterPoint(currentCenterPoint)
//            it.matrix.postScale(factor, factor, currentCenterPoint.x, currentCenterPoint.y)
//            listener?.onComponentChangeSize(it)
//        }
//    }
//
//    fun addComponentWithoutFocus(component: Component, x: Float, y: Float) {
//        if (isLaidOut) {
//            addAndMoveWithoutFocus(component, x, y)
//        } else {
//            post {
//                addAndMoveWithoutFocus(component, x, y)
//            }
//        }
//        listener?.onComponentAdded(getActualComponents())
//    }
//
//    fun addComponent(component: Component, x: Float, y: Float) {
//        if (isLaidOut) {
//            focusedComponent = component
//            addAndMove(component, x, y)
//            listener?.onComponentSelected(component, -1)
//        } else {
//            post {
//                focusedComponent = component
//                addAndMove(component, x, y)
//                listener?.onComponentSelected(component, -1)
//            }
//        }
//        listener?.onComponentAdded(getActualComponents())
//    }
//
//    fun addComponent(component: Component) {
//        if (isLaidOut) {
//            focusedComponent = component
//            addAndMove(component)
//            listener?.onComponentSelected(component, -1)
//        } else {
//            post {
//                focusedComponent = component
//                addAndMove(component)
//                listener?.onComponentSelected(component, -1)
//            }
//        }
//        listener?.onComponentAdded(getActualComponents())
//    }
//
//    fun unSelectComponent() {
//        focusedComponent = null
//        invalidate()
//    }
//
//    fun selectComponent(position: Int) {
//        val selectedComponent =
//            listComponent[position + 1] // +1 because the first component is the background
//        if (!selectedComponent.isVisible) {
//            return
//        }
//        focusedComponent = selectedComponent
//        focusedComponent?.getMappedCenterPoint(currentCenterPoint)
//        invalidate()
//        listener?.onComponentSelected(
//            selectedComponent,
//            position
//        )  // not +1 because we need to update the real position in layer adapter
//    }
//
//    fun swapComponent(fromPosition: Int, toPosition: Int) {
//        // +1 because the first component is the background
//        val fromComponent = listComponent[fromPosition + 1]
//        val toComponent = listComponent[toPosition + 1]
//        listComponent[fromPosition + 1] = toComponent
//        listComponent[toPosition + 1] = fromComponent
//        invalidate()
//    }
//
//    fun rotate(angle: Float) {
//        focusedComponent?.run {
//            getMappedCenterPoint(currentCenterPoint)
//            matrix.postRotate(angle, currentCenterPoint.x, currentCenterPoint.y)
//            listener?.onComponentRotate(this)
//        }
//    }
//
//    fun setOnLogoListener(listener: LogoListener) {
//        this.listener = listener
//    }
//
//    fun executeAction(action: () -> Component) {
//        val component = action()
//        val index = listComponent.indexOfFirst { it.id == component.id }
//        if (index == -1) {
//            listComponent.add(component)
//        } else {
//            listComponent[index] = component
//        }
////        saveStack()
//        invalidate()
//    }
//
//    fun enableColorMode(isEnable: Boolean) {
//        enableColorMode = isEnable
//        if (isEnable) {
//            bitmapCopy = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
//            val canvas = Canvas(bitmapCopy!!)
//            draw(canvas)
//        } else {
//            bitmapCopy = null
//        }
//    }
//
//    fun setLayerData(layer: String, application: MyApplication) {
//        val gson = GsonBuilder()
//            .registerTypeAdapter(Component::class.java, ComponentAdapter())
//            .registerTypeAdapter(Bitmap::class.java, BitmapAdapter(application))
//            .registerTypeAdapter(Matrix::class.java, MatrixAdapter())
//            .serializeSpecialFloatingPointValues()
//            .create()
//        val listType = object : TypeToken<List<Component>>() {}.type
//        val data = gson.fromJson<List<Component>>(layer, listType)
//        listComponent.clear()
//        undoList.clear()
//        listComponent.addAll(data)
//        listComponent.forEach {
//            if (it is TextComponent) {
//                it.applyConfig(context)
//            } else {
//                it.applyCurrentConfig()
//            }
//        }
//        saveStack()
//        invalidate()
//    }
//
//    fun getBitmap(): Bitmap {
//        val bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
//        val canvas = Canvas(bitmap)
//        draw(canvas)
//        return bitmap
//    }
//
//    fun getLayerData(application: MyApplication): String {
//        val gson = GsonBuilder()
//            .registerTypeAdapter(Component::class.java, ComponentAdapter())
//            .registerTypeAdapter(Bitmap::class.java, BitmapAdapter(application))
//            .registerTypeAdapter(Matrix::class.java, MatrixAdapter())
//            .serializeSpecialFloatingPointValues()
//            .create()
//        val listType = object : TypeToken<List<Component>>() {}.type
//        val json = gson.toJson(listComponent, listType)
//        return json
//    }
//
//    private fun handleGetColor(event: MotionEvent?) {
//        when (event?.action) {
//            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
//                val x = event.x.toInt()
//                val y = event.y.toInt()
//                if (x >= 0 && x < (bitmapCopy?.width ?: 0) && y >= 0 && y < (bitmapCopy?.height
//                        ?: 0)
//                ) {
//                    val color = bitmapCopy?.getPixel(x, y) ?: Color.TRANSPARENT
//                    listener?.onGetColor(color)
//                }
//            }
//
//            MotionEvent.ACTION_UP -> {
//                listener?.onUserCancelGetColor()
//            }
//
//            else -> {}
//        }
//    }
//
//    fun saveStack() {
//        undoList.add(listComponent.mapIndexed { index, component ->
//            component.copy(context).apply {
//                this.isVisible = listComponent[index].isVisible
//            }
//        })
//        redoList.clear()
//        listener?.onCanRedo(canRedo())
//        listener?.onCanUndo(canUndo())
//    }
//
//    fun setBackgroundComponent(backGroundComponent: BackGroundComponent) {
//        backGroundComponent.setBound(measuredWidth.toFloat(), measuredHeight.toFloat())
//        if (listComponent.isEmpty()) {
//            listComponent.add(backGroundComponent)
//        } else if (listComponent.first() is BackGroundComponent) {
//            listComponent[0] = backGroundComponent
//        } else {
//            listComponent.add(0, backGroundComponent)
//        }
//    }
//
//    fun undoAction() {
//        if (canUndo().not()) {
//            return
//        }
//        val listItem = undoList.removeAt(undoList.lastIndex)
//        listComponent.clear()
//        if (undoList.isNotEmpty()) {
//            val newList = undoList[undoList.lastIndex]
//            listComponent.addAll(newList.mapIndexed { index, component ->
//                component.copy(context)
//                    .apply {
//                        this.isVisible = newList[index].isVisible
//                    }
//            })
//        }
//        redoList.add(listItem)
//        focusedComponent = null
//        invalidate()
//        listener?.onCanUndo(canUndo())
//        listener?.onCanRedo(redoList.isNotEmpty())
//        listener?.onComponentUnSelected()
//    }
//
//    fun redoAction() {
//        if (canRedo().not()) {
//            return
//        }
//        val newList = redoList.removeAt(redoList.lastIndex)
//        listComponent.clear()
//
//        listComponent.addAll(newList.mapIndexed { index, component ->
//            component.copy(context).apply {
//                this.isVisible = newList[index].isVisible
//            }
//        })
//        undoList.add(newList)
//        focusedComponent = null
//        invalidate()
//        listener?.onCanUndo(canUndo())
//        listener?.onCanRedo(redoList.isNotEmpty())
//        listener?.onComponentUnSelected()
//    }
//
//    fun isEdited() = canUndo()
//
//    private fun canUndo() =
//        (undoList.isEmpty() || undoList.size <= keepStackItem).not()
//
//    private fun canRedo() = redoList.isNotEmpty()
//
//    fun exportImage(): Bitmap {
//        val backGroundComponent = getBackgroundComponent()
//        if (backGroundComponent?.isDefault() == true) {
//            listComponent.removeAt(0)
//        }
//        val bitmap = getBitmap()
//        if (backGroundComponent?.isDefault() == true) {
//            listComponent.add(backGroundComponent)
//        }
//        return bitmap
//    }
//
//    private fun addAndMove(component: Component) {
//        moveToCenter(component)
//        addComponentToList(component)
//        saveStack()
//        invalidate()
//    }
//
//    private fun addAndMove(component: Component, x: Float, y: Float) {
//        component.getMappedCenterPoint(currentCenterPoint)
//        moveX(x, component)
//        moveY(y, component)
//        component.getMappedCenterPoint(currentCenterPoint)
//        addComponentToList(component)
//        saveStack()
//        invalidate()
//    }
//
//    private fun addAndMoveWithoutFocus(component: Component, x: Float, y: Float) {
//        component.getMappedCenterPoint(currentCenterPoint)
//        moveX(x, component)
//        moveY(y, component)
//        component.getMappedCenterPoint(currentCenterPoint)
//        addComponentToList(component)
//    }
//
//    private fun addComponentToList(component: Component) {
//        val index = listComponent.indexOfFirst { it.id == component.id }
//        if (index == -1) {
//            listComponent.add(component)
//        } else {
//            listComponent[index] = component
//        }
//    }
//
//    private fun onActionUp(event: MotionEvent): Boolean {
//        if (needSaveToStack) {
//            needSaveToStack = false
//            saveStack()
//        }
//        if (currentType != ActionType.ZOOM_AND_ROTATION) {
//            if (nextFocusComponent?.contain(event.x, event.y) == true) {
//                focusedComponent = nextFocusComponent
//                focusedComponent?.getMappedCenterPoint(currentCenterPoint)
//                listener?.onComponentSelected(
//                    focusedComponent!!,
//                    nextFocusPosition - 1
//                ) // -1 because the first component is the background
//            } else {
//                focusedComponent = null
//                listener?.onComponentUnSelected()
//            }
//        }
//        nextFocusComponent = null
//        nextFocusPosition = -1
//        oldDistance = 0f
//        currentType = ActionType.NONE
//        invalidate()
//        return true
//    }
//
//    private fun onActionPointerDown(event: MotionEvent): Boolean {
//        return focusedComponent?.let {
//            if (it.contain(event.getX(1), event.getY(1))) {
//                oldDistance = calculateDistance(event)
//                oldRotation = calculateRotation(event)
//                currentType = ActionType.ZOOM_AND_ROTATION
//            }
//            true
//        } ?: false
//    }
//
//    private fun onActionDown(event: MotionEvent): Boolean {
//        if (deleteComponent.contain(event.x, event.y)) {
//            focusedComponent?.let {
//                deleteComponent(it)
//            }
//            return true
//        }
//        if (duplicateComponent.contain(event.x, event.y)) {
//            focusedComponent?.let {
//                listComponent.add(it.copyWithoutId(context))
//                listener?.onComponentAdded(getActualComponents())
//            }
//            invalidate()
//            return true
//        }
//        val endIndex = listComponent.size - 1
//        for (i in endIndex downTo 0) {
//            val item = listComponent[i]
//            if (item.contain(
//                    event.x,
//                    event.y
//                ) && (item is BackGroundComponent).not() && item.isVisible
//            ) {
//                currentType = ActionType.DRAG
//                downX = event.x
//                downY = event.y
//                nextFocusComponent = item
//                nextFocusPosition = i
//                invalidate()
//                return true
//            }
//        }
//        nextFocusComponent = null
//        nextFocusPosition = -1
//        invalidate()
//        return true
//    }
//
//    private fun onActionMove(event: MotionEvent): Boolean {
//        if (focusedComponent == null) return false
//        if (focusedComponent != nextFocusComponent) return false
//        needSaveToStack = true
//        when (currentType) {
//            ActionType.DRAG -> move(event, focusedComponent!!)
//            ActionType.ZOOM_AND_ROTATION -> scaleAndRotation(event)
//            else -> {
//                needSaveToStack = true
//            }
//        }
//        invalidate()
//        return true
//    }
//
//    private fun deleteComponent(component: Component) {
//        focusedComponent = null
//        listComponent.remove(component)
//        listener?.onComponentRemove()
//        saveStack()
//        invalidate()
//    }
//
//    private fun scaleAndRotation(event: MotionEvent) {
//        val newDistance = calculateDistance(event)
//        val scaleFactor = abs(newDistance / oldDistance)
//        scale(scaleFactor)
//        oldDistance = newDistance
//        if (event.pointerCount >= 2) {
//            val newRotation = calculateRotation(event)
//            rotate(newRotation - oldRotation)
//            oldRotation = newRotation
//        }
//    }
//
//    private fun moveToCenter(component: Component) {
//        component.getMappedCenterPoint(currentCenterPoint)
//        moveX(measuredWidth / 2f - currentCenterPoint.x, component)
//        moveY(measuredHeight / 2f - currentCenterPoint.y, component)
//        component.getMappedCenterPoint(currentCenterPoint)
//    }
//
//    private fun move(event: MotionEvent, component: Component) {
//        moveX(event.x - downX, component)
//        moveY(event.y - downY, component)
//        component.getMappedCenterPoint(currentCenterPoint)
//        downX = event.x
//        downY = event.y
//    }
//
//    private fun moveX(distance: Float, component: Component) {
//        val tempMatrix = Matrix()
//        tempMatrix.postTranslate(distance, 0f)
//        val newCenterPoint = FloatArray(2)
//        tempMatrix.mapPoints(
//            newCenterPoint,
//            floatArrayOf(currentCenterPoint.x, currentCenterPoint.y)
//        )
//        val moveX: Float = if (newCenterPoint[0] < 0) {
//            distance - newCenterPoint[0]
//        } else if (newCenterPoint[0] > measuredWidth) {
//            distance - (newCenterPoint[0] - measuredWidth)
//        } else {
//            distance
//        }
//        component.matrix.postTranslate(moveX, 0f)
//    }
//
//    private fun moveY(distance: Float, component: Component) {
//        val tempMatrix = Matrix()
//        tempMatrix.postTranslate(0f, distance)
//        val newCenterPoint = FloatArray(2)
//        tempMatrix.mapPoints(
//            newCenterPoint,
//            floatArrayOf(currentCenterPoint.x, currentCenterPoint.y)
//        )
//        val moveY: Float = if (newCenterPoint[1] < 0) {
//            distance - newCenterPoint[1]
//        } else if (newCenterPoint[1] > measuredHeight) {
//            distance - (newCenterPoint[1] - measuredHeight)
//        } else {
//            distance
//        }
//        component.matrix.postTranslate(0f, moveY)
//    }
//
//    private fun drawComponent(canvas: Canvas) {
//        listComponent.forEach {
//            if (it.isVisible) {
//                it.draw(canvas)
//            }
//        }
//    }
//
//    private fun drawComponentBorder(canvas: Canvas) {
//        focusedComponent?.let { component ->
//            if (component.needDrawBound.not()) {
//                return
//            }
//            val bounds = component.getBoundPoint()
//            component.getMappedPoint(borderPoint, bounds)
//            canvas.drawLine(
//                borderPoint[0],
//                borderPoint[1],
//                borderPoint[2],
//                borderPoint[3],
//                borderPaint
//            )
//            canvas.drawLine(
//                borderPoint[2],
//                borderPoint[3],
//                borderPoint[4],
//                borderPoint[5],
//                borderPaint
//            )
//            canvas.drawLine(
//                borderPoint[4],
//                borderPoint[5],
//                borderPoint[6],
//                borderPoint[7],
//                borderPaint
//            )
//            canvas.drawLine(
//                borderPoint[6],
//                borderPoint[7],
//                borderPoint[0],
//                borderPoint[1],
//                borderPaint
//            )
//            drawComponentFunction(canvas, deleteComponent, borderPoint[0], borderPoint[1])
//            drawComponentFunction(canvas, duplicateComponent, borderPoint[2], borderPoint[3])
//        }
//    }
//
//    private fun drawComponentFunction(
//        canvas: Canvas,
//        icon: IconFunctionComponent,
//        centerX: Float,
//        centerY: Float
//    ) {
//        icon.matrix.reset()
//        icon.matrix.postTranslate(
//            centerX - icon.getWidth() / 2,
//            centerY - icon.getHeight() / 2
//        )
//        icon.draw(canvas)
//    }
//
//    private fun calculateDistance(event: MotionEvent): Float {
//        if (event.pointerCount < 2) return 0f
//        val x1 = event.getX(0)
//        val y1 = event.getY(0)
//        val x2 = event.getX(1)
//        val y2 = event.getY(1)
//        val dx = (x1 - x2).toDouble()
//        val dy = (y1 - y2).toDouble()
//        return sqrt(dx * dx + dy * dy).toFloat()
//    }
//
//    private fun calculateRotation(event: MotionEvent): Float {
//        if (event.pointerCount < 2) return 0f
//        val x1 = event.getX(0)
//        val y1 = event.getY(0)
//        val x2 = event.getX(1)
//        val y2 = event.getY(1)
//        return calculateRotation(x1, y1, x2, y2)
//    }
//
//    private fun calculateRotation(x1: Float, y1: Float, x2: Float, y2: Float): Float {
//        val dx = (x1 - x2).toDouble()
//        val dy = (y1 - y2).toDouble()
//        return Math.toDegrees(atan2(dy, dx)).toFloat()
//    }
//
//    fun getBackgroundComponent() = listComponent.firstOrNull() as? BackGroundComponent
//
//    fun getSizeComponent(): Int {
//        return listComponent.size
//    }
//
//    fun hideComponent(position: Int) {
//        listComponent[position + 1].isVisible = false
//        focusedComponent = null
//        saveStack()
//        invalidate()
//    }
//
//    fun showComponent(position: Int) {
//        listComponent[position + 1].isVisible = true
//        saveStack()
//        invalidate()
//    }
//
//    fun deleteComponent(position: Int) {
//        deleteComponent(listComponent[position + 1])
//    }
//
//    fun getActualComponents(): List<Component> {
//        val tempList = mutableListOf<Component>()
//        tempList.addAll(listComponent)
//        tempList.removeAt(0)
//        return tempList
//    }
//
//    fun clearStack() {
//        undoList.clear()
//        redoList.clear()
//    }
//}
